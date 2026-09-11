package com.reloop.orders.repository;

import com.reloop.orders.domain.FulfillmentOrder;
import com.reloop.common.jpa.ReloopRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class FulfillmentOrderRepository implements ReloopRepository<FulfillmentOrder, UUID> {

    @PersistenceContext
    EntityManager entityManager;

    public List<FulfillmentOrder> findByMasterOrderId(UUID masterOrderId) {
        return list("masterOrderId", masterOrderId);
    }

    public List<FulfillmentOrder> findBySellerId(Long sellerId) {
        return list("sellerId", sellerId);
    }

    /**
     * fulfillment_orders.seller_id references sellers.id (store profile), while
     * RBAC operates on user ids. Resolves the owning user via a native lookup
     * (no Java dependency on the sellers module, keeping the module boundary).
     */
    public Long resolveSellerUserId(Long sellerProfileId) {
        Object result = entityManager
                .createNativeQuery("SELECT user_id FROM sellers WHERE id = ?1")
                .setParameter(1, sellerProfileId)
                .getResultList().stream().findFirst().orElse(null);
        return toLong(result);
    }

    /** Reverse lookup: user id -> seller profile id (falls back to the user id itself). */
    public Long resolveSellerProfileId(Long sellerUserId) {
        Object result = entityManager
                .createNativeQuery("SELECT id FROM sellers WHERE user_id = ?1")
                .setParameter(1, sellerUserId)
                .getResultList().stream().findFirst().orElse(null);
        return result == null ? sellerUserId : toLong(result);
    }

    private static Long toLong(Object result) {
        if (result == null) return null;
        if (result instanceof Number n) return n.longValue();
        return Long.parseLong(result.toString());
    }
}
