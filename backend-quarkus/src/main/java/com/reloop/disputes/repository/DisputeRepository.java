package com.reloop.disputes.repository;

import com.reloop.disputes.domain.Dispute;
import com.reloop.common.jpa.ReloopRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class DisputeRepository implements ReloopRepository<Dispute, UUID> {

    public List<Dispute> findByBuyerId(Long buyerId) {
        return list("buyerId", buyerId);
    }

    public List<Dispute> findBySellerId(Long sellerId) {
        return list("sellerId", sellerId);
    }

    public Optional<Dispute> findByFulfillmentOrderIdAndStatusOpen(UUID fulfillmentOrderId) {
        return find("fulfillmentOrderId = ?1 and status = ?2", fulfillmentOrderId, Dispute.DisputeStatus.OPEN)
                .firstResultOptional();
    }
}
