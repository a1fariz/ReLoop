package com.reloop.disputes.service;

import com.reloop.common.exception.BusinessException;
import com.reloop.disputes.domain.Dispute;
import com.reloop.disputes.dto.DisputeDtos;
import com.reloop.disputes.repository.DisputeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class DisputeService {
    private final DisputeRepository disputeRepository;

    public DisputeService(DisputeRepository disputeRepository) {
        this.disputeRepository = disputeRepository;
    }

    @Transactional
    public DisputeDtos.DisputeResponse createDispute(Long buyerId, DisputeDtos.CreateDisputeRequest request) {
        Dispute dispute = new Dispute(
                request.fulfillmentOrderId(),
                buyerId,
                request.sellerId(),
                request.reason(),
                request.claimDescription()
        );
        dispute = disputeRepository.save(dispute);
        return toResponse(dispute);
    }

    @Transactional
    public DisputeDtos.DisputeResponse resolveDispute(UUID disputeId, Long adminId, DisputeDtos.ResolveDisputeRequest request) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new BusinessException("Dispute not found", "DISPUTE_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (dispute.getStatus() == Dispute.DisputeStatus.RESOLVED || dispute.getStatus() == Dispute.DisputeStatus.CLOSED) {
            throw new BusinessException("Dispute is already resolved", "ALREADY_RESOLVED", HttpStatus.CONFLICT);
        }

        Dispute.ResolutionType type = Dispute.ResolutionType.valueOf(request.resolutionType().toUpperCase());
        dispute.setResolutionType(type);
        dispute.setStatus(Dispute.DisputeStatus.RESOLVED);
        dispute.setBuyerRefundAmount(request.buyerRefundAmount() != null ? request.buyerRefundAmount() : BigDecimal.ZERO);
        dispute.setSellerPayoutAmount(request.sellerPayoutAmount() != null ? request.sellerPayoutAmount() : BigDecimal.ZERO);
        dispute.setResolutionNotes(request.resolutionNotes());
        dispute.setResolvedByAdminId(adminId);
        dispute.setResolvedAt(Instant.now());

        dispute = disputeRepository.save(dispute);
        return toResponse(dispute);
    }

    @Transactional(readOnly = true)
    public List<DisputeDtos.DisputeResponse> getBuyerDisputes(Long buyerId) {
        return disputeRepository.findByBuyerId(buyerId).stream()
                .map(this::toResponse)
                .toList();
    }

    private DisputeDtos.DisputeResponse toResponse(Dispute d) {
        return new DisputeDtos.DisputeResponse(
                d.getId(),
                d.getFulfillmentOrderId(),
                d.getBuyerId(),
                d.getSellerId(),
                d.getReason(),
                d.getClaimDescription(),
                d.getStatus().name(),
                d.getResolutionType() != null ? d.getResolutionType().name() : null,
                d.getBuyerRefundAmount(),
                d.getSellerPayoutAmount(),
                d.getCreatedAt()
        );
    }
}
