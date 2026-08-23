package com.reloop.inspections.repository;

import com.reloop.inspections.domain.TechnicalInspection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TechnicalInspectionRepository extends JpaRepository<TechnicalInspection, UUID> {
    Optional<TechnicalInspection> findTopByUnitIdOrderByCreatedAtDesc(UUID unitId);
}
