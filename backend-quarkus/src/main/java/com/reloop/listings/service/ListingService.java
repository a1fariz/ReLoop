package com.reloop.listings.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.reloop.common.exception.BusinessException;
import com.reloop.listings.domain.Listing;
import com.reloop.listings.dto.CreateListingRequest;
import com.reloop.listings.dto.ListingDto;
import com.reloop.listings.repository.ListingRepository;
import com.reloop.units.domain.ProductUnit;
import com.reloop.units.repository.ProductUnitRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.hibernate.exception.ConstraintViolationException;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ListingService {
    private final ListingRepository listingRepository;
    private final ProductUnitRepository productUnitRepository;
    private final ObjectMapper objectMapper;

    @PersistenceContext
    EntityManager entityManager;

    @Inject
    public ListingService(ListingRepository listingRepository, ProductUnitRepository productUnitRepository,
                          ObjectMapper objectMapper) {
        this.listingRepository = listingRepository;
        this.productUnitRepository = productUnitRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public List<ListingDto> getActiveListings() {
        return listingRepository.findByStatus(Listing.ListingStatus.ACTIVE).stream()
                .map(ListingService::toDto)
                .toList();
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public ListingDto getListingById(UUID id) {
        return listingRepository.findByIdOptional(id)
                .map(ListingService::toDto)
                .orElseThrow(() -> new BusinessException("Listing not found", "LISTING_NOT_FOUND", 404));
    }

    /**
     * Creates an ACTIVE listing for a unit the seller owns. The single-active-
     * listing-per-unit invariant is pre-checked and backed by the partial unique
     * index uq_single_active_listing_per_unit.
     */
    @Transactional
    public ListingDto createListing(Long sellerId, CreateListingRequest request) {
        ProductUnit unit = productUnitRepository.findByIdOptional(request.unitId())
                .orElseThrow(() -> new BusinessException("Product unit not found", "UNIT_NOT_FOUND", 404));

        if (!unit.getCurrentOwnerId().equals(sellerId)) {
            throw new BusinessException("You do not own this product unit", "LISTING_NOT_OWNED", 403);
        }
        if (unit.getStatus() != ProductUnit.UnitStatus.AVAILABLE
                && unit.getStatus() != ProductUnit.UnitStatus.READY_FOR_LISTING) {
            throw new BusinessException("Product unit is not listable in its current state", "UNIT_NOT_LISTABLE", 409);
        }
        if (listingRepository.findActiveByUnitId(unit.getId()).isPresent()) {
            throw new BusinessException("Unit already has an active listing", "LISTING_ALREADY_ACTIVE", 409);
        }

        Listing listing = new Listing(unit.getId(), sellerId, request.title(),
                request.description(), request.askingPrice(), unit.getGrade() != null ? unit.getGrade() : "N/A");
        listing.setImages(normalizeImages(request.images()));
        try {
            listing = listingRepository.save(listing);
        } catch (PersistenceException e) {
            if (hasConstraint(e, "uq_single_active_listing_per_unit")) {
                throw new BusinessException("Unit already has an active listing", "LISTING_ALREADY_ACTIVE", 409);
            }
            throw e;
        }

        unit.setStatus(ProductUnit.UnitStatus.LISTED);
        productUnitRepository.save(unit);

        return toDto(listing);
    }

    @Transactional
    public ListingDto pauseListing(UUID listingId, Long sellerId) {
        Listing listing = loadOwned(listingId, sellerId);
        if (listing.getStatus() != Listing.ListingStatus.ACTIVE) {
            throw new BusinessException("Only ACTIVE listings can be paused", "INVALID_LISTING_STATE", 409);
        }
        listing.setStatus(Listing.ListingStatus.PAUSED);
        listing = listingRepository.save(listing);

        productUnitRepository.findByIdOptional(listing.getUnitId()).ifPresent(unit -> {
            if (unit.getStatus() == ProductUnit.UnitStatus.LISTED) {
                unit.setStatus(ProductUnit.UnitStatus.READY_FOR_LISTING);
                productUnitRepository.save(unit);
            }
        });
        return toDto(listing);
    }

    @Transactional
    public ListingDto resumeListing(UUID listingId, Long sellerId) {
        Listing listing = loadOwned(listingId, sellerId);
        if (listing.getStatus() != Listing.ListingStatus.PAUSED) {
            throw new BusinessException("Only PAUSED listings can be resumed", "INVALID_LISTING_STATE", 409);
        }
        if (listingRepository.findActiveByUnitId(listing.getUnitId()).isPresent()) {
            throw new BusinessException("Unit already has an active listing", "LISTING_ALREADY_ACTIVE", 409);
        }
        listing.setStatus(Listing.ListingStatus.ACTIVE);
        listing = listingRepository.save(listing);

        productUnitRepository.findByIdOptional(listing.getUnitId()).ifPresent(unit -> {
            if (unit.getStatus() == ProductUnit.UnitStatus.READY_FOR_LISTING
                    || unit.getStatus() == ProductUnit.UnitStatus.AVAILABLE) {
                unit.setStatus(ProductUnit.UnitStatus.LISTED);
                productUnitRepository.save(unit);
            }
        });
        return toDto(listing);
    }

    private Listing loadOwned(UUID listingId, Long sellerId) {
        Listing listing = listingRepository.findByIdOptional(listingId)
                .orElseThrow(() -> new BusinessException("Listing not found", "LISTING_NOT_FOUND", 404));
        if (!listing.getSellerId().equals(sellerId)) {
            throw new BusinessException("Listing does not belong to this seller", "LISTING_FORBIDDEN", 403);
        }
        return listing;
    }

    /**
     * Partial update for a seller-owned listing: only non-null fields change.
     * gradeSnapshot is intentionally immutable (certified grade at listing time).
     */
    @Transactional
    public ListingDto updateListing(UUID listingId, Long sellerId, com.reloop.listings.dto.UpdateListingRequest request) {
        Listing listing = loadOwned(listingId, sellerId);
        if (request.title() != null) {
            listing.setTitle(request.title());
        }
        if (request.description() != null) {
            listing.setDescription(request.description());
        }
        if (request.askingPrice() != null) {
            listing.setAskingPrice(request.askingPrice());
        }
        if (request.images() != null) {
            listing.setImages(normalizeImages(request.images()));
        }
        return toDto(listingRepository.save(listing));
    }

    /** Marketplace search with price/grade filters, sorting and pagination. */
    @Transactional(Transactional.TxType.SUPPORTS)
    public com.reloop.common.dto.Page<ListingDto> searchListings(
            String query, java.math.BigDecimal minPrice, java.math.BigDecimal maxPrice, String grade,
            String sort, int page, int size) {
        io.quarkus.panache.common.Sort order = switch (sort == null ? "" : sort) {
            case "priceAsc" -> io.quarkus.panache.common.Sort.by("askingPrice", io.quarkus.panache.common.Sort.Direction.Ascending);
            case "priceDesc" -> io.quarkus.panache.common.Sort.by("askingPrice", io.quarkus.panache.common.Sort.Direction.Descending);
            default -> io.quarkus.panache.common.Sort.descending("createdAt");
        };
        var results = listingRepository.search(com.reloop.listings.domain.Listing.ListingStatus.ACTIVE,
                query, minPrice, maxPrice, grade, order);
        // PanacheQuery from the ORM module
        long total = results.count();
        List<ListingDto> items = results.page(io.quarkus.panache.common.Page.of(page, size)).list()
                .stream()
                .map(ListingService::toDto)
                .toList();
        return new com.reloop.common.dto.Page<>(items, total, page, size);
    }

    /** Accepts "[]" (default) or a JSON array of image URLs; rejects anything else. */
    private String normalizeImages(String images) {
        if (images == null || images.isBlank()) {
            return "[]";
        }
        try {
            com.fasterxml.jackson.databind.JsonNode tree = objectMapper.readTree(images);
            if (tree.isArray()) {
                return images;
            }
        } catch (JsonProcessingException ignored) {
            // fall through
        }
        throw new com.reloop.common.exception.BusinessException("images must be a JSON array of URLs", "INVALID_IMAGES", 400);
    }

    private static boolean hasConstraint(Throwable e, String constraint) {
        while (e != null) {
            if (e instanceof ConstraintViolationException cve
                    && cve.getConstraintName() != null
                    && cve.getConstraintName().toLowerCase().contains(constraint)) {
                return true;
            }
            e = e.getCause();
        }
        return false;
    }

    public ListingDto toDto(Listing listing) {
        return new ListingDto(
                listing.getId(),
                listing.getUnitId(),
                listing.getSellerId(),
                sellerStoreName(listing.getSellerId()),
                listing.getTitle(),
                listing.getDescription(),
                listing.getAskingPrice(),
                listing.getStatus().name(),
                listing.getGradeSnapshot(),
                listing.getImages()
        );
    }

    private String sellerStoreName(Long sellerProfileId) {
        if (sellerProfileId == null || entityManager == null) {
            return null;
        }
        try {
            Object name = entityManager
                    .createNativeQuery("SELECT store_name FROM sellers WHERE id = ?1")
                    .setParameter(1, sellerProfileId)
                    .getResultList().stream().findFirst().orElse(null);
            return name != null ? name.toString() : null;
        } catch (RuntimeException e) {
            return null;
        }
    }
}
