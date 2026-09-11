package com.reloop.returns.repository;

import com.reloop.common.jpa.ReloopRepository;
import com.reloop.returns.domain.ReturnAuthorization;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ReturnAuthorizationRepository implements ReloopRepository<ReturnAuthorization, UUID> {

    public List<ReturnAuthorization> findByFulfillmentOrderId(UUID fulfillmentOrderId) {
        return list("fulfillmentOrderId", fulfillmentOrderId);
    }

    public boolean existsOpenByFulfillmentOrderId(UUID fulfillmentOrderId) {
        return count("fulfillmentOrderId = ?1 AND status IN ?2",
                fulfillmentOrderId, List.of(ReturnAuthorization.openStatuses())) > 0;
    }
}
