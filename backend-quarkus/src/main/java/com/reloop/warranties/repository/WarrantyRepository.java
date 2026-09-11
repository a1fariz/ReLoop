package com.reloop.warranties.repository;

import com.reloop.warranties.domain.Warranty;
import com.reloop.common.jpa.ReloopRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class WarrantyRepository implements ReloopRepository<Warranty, UUID> {

    public List<Warranty> findByOwnerId(Long ownerId) {
        return list("ownerId", ownerId);
    }

    public Optional<Warranty> findByUnitIdAndIsVoidedFalse(UUID unitId) {
        return find("unitId = ?1 AND isVoided = false", unitId).firstResultOptional();
    }
}
