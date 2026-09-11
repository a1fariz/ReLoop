package com.reloop.checkout.repository;

import com.reloop.checkout.domain.IdempotencyKeyRecord;
import com.reloop.common.jpa.ReloopRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class IdempotencyKeyRecordRepository implements ReloopRepository<IdempotencyKeyRecord, UUID> {

    public Optional<IdempotencyKeyRecord> findByUserIdAndIdempotencyKeyAndEndpointForUpdate(
            Long userId, String idempotencyKey, String endpoint) {
        return getEntityManager()
                .createQuery("""
                        SELECT k FROM IdempotencyKeyRecord k
                        WHERE k.userId = :userId AND k.idempotencyKey = :idempotencyKey AND k.endpoint = :endpoint
                        """, IdempotencyKeyRecord.class)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .setParameter("userId", userId)
                .setParameter("idempotencyKey", idempotencyKey)
                .setParameter("endpoint", endpoint)
                .getResultStream()
                .findFirst();
    }
}
