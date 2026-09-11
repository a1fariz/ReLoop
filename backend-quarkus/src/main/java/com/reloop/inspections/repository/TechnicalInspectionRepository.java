package com.reloop.inspections.repository;

import com.reloop.inspections.domain.TechnicalInspection;
import com.reloop.common.jpa.ReloopRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class TechnicalInspectionRepository implements ReloopRepository<TechnicalInspection, UUID> {

    public Optional<TechnicalInspection> findTopByUnitIdOrderByCreatedAtDesc(UUID unitId) {
        return find("unitId = ?1 ORDER BY createdAt DESC", unitId).firstResultOptional();
    }
}
