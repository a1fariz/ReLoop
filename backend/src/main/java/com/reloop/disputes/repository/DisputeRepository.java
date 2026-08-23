package com.reloop.disputes.repository;

import com.reloop.disputes.domain.Dispute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DisputeRepository extends JpaRepository<Dispute, UUID> {
    List<Dispute> findByBuyerId(Long buyerId);
    List<Dispute> findBySellerId(Long sellerId);
}
