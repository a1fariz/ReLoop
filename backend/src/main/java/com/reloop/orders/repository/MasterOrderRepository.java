package com.reloop.orders.repository;

import com.reloop.orders.domain.MasterOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MasterOrderRepository extends JpaRepository<MasterOrder, UUID> {
    Optional<MasterOrder> findByOrderNumber(String orderNumber);
    List<MasterOrder> findByBuyerId(Long buyerId);
}
