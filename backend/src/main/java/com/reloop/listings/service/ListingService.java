package com.reloop.listings.service;

import com.reloop.common.exception.BusinessException;
import com.reloop.listings.domain.Listing;
import com.reloop.listings.dto.ListingDto;
import com.reloop.listings.repository.ListingRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ListingService {
    private final ListingRepository listingRepository;

    public ListingService(ListingRepository listingRepository) {
        this.listingRepository = listingRepository;
    }

    @Transactional(readOnly = true)
    public List<ListingDto> getActiveListings() {
        return listingRepository.findByStatus(Listing.ListingStatus.ACTIVE).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ListingDto getListingById(UUID id) {
        return listingRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new BusinessException("Listing not found", "LISTING_NOT_FOUND", HttpStatus.NOT_FOUND));
    }

    private ListingDto toDto(Listing listing) {
        return new ListingDto(
                listing.getId(),
                listing.getUnitId(),
                listing.getSellerId(),
                listing.getTitle(),
                listing.getDescription(),
                listing.getAskingPrice(),
                listing.getStatus().name(),
                listing.getGradeSnapshot()
        );
    }
}
