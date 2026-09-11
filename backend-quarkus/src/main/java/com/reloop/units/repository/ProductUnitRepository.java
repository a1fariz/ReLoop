package com.reloop.units.repository;

import com.reloop.units.domain.ProductUnit;
import com.reloop.common.jpa.ReloopRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ProductUnitRepository implements ReloopRepository<ProductUnit, UUID> {

    public Optional<ProductUnit> findBySerialNumber(String serialNumber) {
        return find("serialNumber", serialNumber).firstResultOptional();
    }

    public Optional<ProductUnit> findByIdForUpdate(UUID id) {
        return Optional.ofNullable(getEntityManager().find(ProductUnit.class, id, LockModeType.PESSIMISTIC_WRITE));
    }
}
