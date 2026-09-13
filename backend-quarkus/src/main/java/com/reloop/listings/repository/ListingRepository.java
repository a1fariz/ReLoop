package com.reloop.listings.repository;

import com.reloop.listings.domain.Listing;
import com.reloop.common.jpa.ReloopRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ListingRepository implements ReloopRepository<Listing, UUID> {

    public List<Listing> findByStatus(Listing.ListingStatus status) {
        return list("status", status);
    }

    public Optional<Listing> findActiveByUnitId(UUID unitId) {
        return find("unitId = ?1 AND status = ?2", unitId, Listing.ListingStatus.ACTIVE).firstResultOptional();
    }

    /** Marketplace search: status + optional keyword, price range and grade, with sorting. */
    public io.quarkus.hibernate.orm.panache.PanacheQuery<Listing> search(
            Listing.ListingStatus status, String query, java.math.BigDecimal minPrice, java.math.BigDecimal maxPrice,
            String grade, io.quarkus.panache.common.Sort sort) {
        java.util.Map<String, Object> params = new java.util.HashMap<>();
        StringBuilder q = new StringBuilder("status = :status");
        params.put("status", status);
        if (query != null && !query.isBlank()) {
            q.append(" AND (lower(title) LIKE :query OR lower(description) LIKE :query)");
            params.put("query", "%" + query.toLowerCase() + "%");
        }
        if (minPrice != null) {
            q.append(" AND askingPrice >= :minPrice");
            params.put("minPrice", minPrice);
        }
        if (maxPrice != null) {
            q.append(" AND askingPrice <= :maxPrice");
            params.put("maxPrice", maxPrice);
        }
        if (grade != null && !grade.isBlank()) {
            q.append(" AND gradeSnapshot = :grade");
            params.put("grade", grade);
        }
        return find(q.toString(), sort, params);
    }

    public Optional<Listing> findByIdForUpdate(UUID id) {
        return Optional.ofNullable(getEntityManager().find(Listing.class, id, LockModeType.PESSIMISTIC_WRITE));
    }
}
