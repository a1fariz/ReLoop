package com.reloop.orders.repository;

import com.reloop.orders.domain.FulfillmentOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FulfillmentOrderRepository extends JpaRepository<FulfillmentOrder, UUID> {
    List<FulfillmentOrder> findByMasterOrderId(UUID masterOrderId);
    List<FulfillmentOrder> findBySellerId(Long sellerId);
}
