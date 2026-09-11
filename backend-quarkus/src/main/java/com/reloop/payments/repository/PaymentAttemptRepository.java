package com.reloop.payments.repository;

import com.reloop.common.jpa.ReloopRepository;
import com.reloop.payments.domain.PaymentAttempt;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class PaymentAttemptRepository implements ReloopRepository<PaymentAttempt, UUID> {

    public List<PaymentAttempt> findByMasterOrderId(UUID masterOrderId) {
        return list("masterOrderId", io.quarkus.panache.common.Sort.descending("createdAt"), masterOrderId);
    }

    public Optional<PaymentAttempt> findByIdempotencyKey(String idempotencyKey) {
        return find("idempotencyKey", idempotencyKey).firstResultOptional();
    }

    public Optional<PaymentAttempt> findByGatewayReference(String gatewayReference) {
        return find("gatewayReference", gatewayReference).firstResultOptional();
    }
}
