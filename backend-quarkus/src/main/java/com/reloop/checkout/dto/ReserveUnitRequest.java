package com.reloop.checkout.dto;

import java.util.UUID;

public class ReserveUnitRequest {
    private UUID unitId;
    private UUID listingId;

    public ReserveUnitRequest() {}

    public ReserveUnitRequest(UUID unitId, UUID listingId) {
        this.unitId = unitId;
        this.listingId = listingId;
    }

    public UUID getUnitId() { return unitId; }
    public void setUnitId(UUID unitId) { this.unitId = unitId; }
    public UUID getListingId() { return listingId; }
    public void setListingId(UUID listingId) { this.listingId = listingId; }
}
