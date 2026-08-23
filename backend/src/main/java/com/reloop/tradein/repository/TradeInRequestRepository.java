package com.reloop.tradein.repository;

import com.reloop.tradein.domain.TradeInRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TradeInRequestRepository extends JpaRepository<TradeInRequest, UUID> {
    List<TradeInRequest> findByUserId(Long userId);
}
