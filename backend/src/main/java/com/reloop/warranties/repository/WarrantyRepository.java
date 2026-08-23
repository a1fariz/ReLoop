package com.reloop.warranties.repository;

import com.reloop.warranties.domain.Warranty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WarrantyRepository extends JpaRepository<Warranty, UUID> {
    List<Warranty> findByOwnerId(Long ownerId);
    Optional<Warranty> findByUnitIdAndIsVoidedFalse(UUID unitId);
}
