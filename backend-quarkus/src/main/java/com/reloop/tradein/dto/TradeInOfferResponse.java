package com.reloop.tradein.dto;

import java.math.BigDecimal;

public record TradeInOfferResponse(
    BigDecimal estimatedOffer,
    BigDecimal baseDepreciatedValue,
    BigDecimal conditionMultiplier,
    BigDecimal batteryMultiplier,
    BigDecimal accessoriesMultiplier,
    BigDecimal platformMarginRate
) {}
