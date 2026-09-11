package com.reloop.checkout.repository;

import com.reloop.checkout.domain.IdempotencyKeyRecord;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface IdempotencyKeyRecordRepository extends JpaRepository<IdempotencyKeyRecord, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT k FROM IdempotencyKeyRecord k WHERE k.userId = :userId AND k.idempotencyKey = :idempotencyKey AND k.endpoint = :endpoint")
    Optional<IdempotencyKeyRecord> findByUserIdAndIdempotencyKeyAndEndpointForUpdate(Long userId, String idempotencyKey, String endpoint);
}
