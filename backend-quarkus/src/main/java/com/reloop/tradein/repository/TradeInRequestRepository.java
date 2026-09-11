package com.reloop.tradein.repository;

import com.reloop.tradein.domain.TradeInRequest;
import com.reloop.common.jpa.ReloopRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class TradeInRequestRepository implements ReloopRepository<TradeInRequest, UUID> {

    public List<TradeInRequest> findByUserId(Long userId) {
        return list("userId", userId);
    }
}
