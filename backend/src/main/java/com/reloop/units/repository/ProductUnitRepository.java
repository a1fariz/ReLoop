package com.reloop.units.repository;

import com.reloop.units.domain.ProductUnit;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductUnitRepository extends JpaRepository<ProductUnit, UUID> {
    Optional<ProductUnit> findBySerialNumber(String serialNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM ProductUnit u WHERE u.id = :id")
    Optional<ProductUnit> findByIdForUpdate(UUID id);
}
