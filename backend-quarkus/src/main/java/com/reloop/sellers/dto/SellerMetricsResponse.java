package com.reloop.sellers.dto;

import java.math.BigDecimal;

public record SellerMetricsResponse(
    Long sellerId,
    String storeName,
    String storeSlug,
    BigDecimal reputationScore,
    BigDecimal returnRate,
    BigDecimal disputeRate,
    BigDecimal responseRate,
    int completedOrders,
    int activeDisputes,
    String kycStatus
) {}
