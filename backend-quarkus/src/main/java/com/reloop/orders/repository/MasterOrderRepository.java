package com.reloop.orders.repository;

import com.reloop.orders.domain.MasterOrder;
import com.reloop.common.jpa.ReloopRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class MasterOrderRepository implements ReloopRepository<MasterOrder, UUID> {

    public Optional<MasterOrder> findByOrderNumber(String orderNumber) {
        return find("orderNumber", orderNumber).firstResultOptional();
    }

    public List<MasterOrder> findByBuyerId(Long buyerId) {
        return list("buyerId", buyerId);
    }
}
