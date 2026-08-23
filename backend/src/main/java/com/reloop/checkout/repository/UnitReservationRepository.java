package com.reloop.checkout.repository;

import com.reloop.checkout.domain.UnitReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UnitReservationRepository extends JpaRepository<UnitReservation, UUID> {
    Optional<UnitReservation> findByToken(UUID token);
    Optional<UnitReservation> findByUnitIdAndStatus(UUID unitId, UnitReservation.ReservationStatus status);

    @Query(value = "SELECT * FROM unit_reservations WHERE status = 'ACTIVE' AND expires_at < :now ORDER BY id ASC FOR UPDATE SKIP LOCKED LIMIT 100", nativeQuery = true)
    List<UnitReservation> findExpiredActiveReservations(Instant now);
}
