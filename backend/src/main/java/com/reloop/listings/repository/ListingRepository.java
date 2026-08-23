package com.reloop.listings.repository;

import com.reloop.listings.domain.Listing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ListingRepository extends JpaRepository<Listing, UUID> {
    List<Listing> findByStatus(Listing.ListingStatus status);
}
