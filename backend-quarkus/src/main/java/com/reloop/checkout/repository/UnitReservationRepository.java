package com.reloop.checkout.repository;

import com.reloop.checkout.domain.UnitReservation;
import com.reloop.common.jpa.ReloopRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class UnitReservationRepository implements ReloopRepository<UnitReservation, UUID> {

    @PersistenceContext
    EntityManager entityManager;

    public Optional<UnitReservation> findByToken(UUID token) {
        return find("token", token).firstResultOptional();
    }

    public Optional<UnitReservation> findByTokenForUpdate(UUID token) {
        return getEntityManager()
                .createQuery("SELECT r FROM UnitReservation r WHERE r.token = :token", UnitReservation.class)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .setParameter("token", token)
                .getResultStream()
                .findFirst();
    }

    public Optional<UnitReservation> findByUnitIdAndStatus(UUID unitId, UnitReservation.ReservationStatus status) {
        return find("unitId = ?1 AND status = ?2", unitId, status).firstResultOptional();
    }

    /**
     * Claims expired leases with FOR UPDATE SKIP LOCKED so concurrent reaper
     * instances never reap the same reservation twice.
     */
    @SuppressWarnings("unchecked")
    public List<UnitReservation> findExpiredActiveReservations(Instant now) {
        return entityManager
                .createNativeQuery("""
                        SELECT * FROM unit_reservations
                        WHERE status = 'ACTIVE' AND expires_at < :now
                        ORDER BY id ASC
                        FOR UPDATE SKIP LOCKED
                        LIMIT 100
                        """, UnitReservation.class)
                .setParameter("now", now)
                .getResultList();
    }
}
