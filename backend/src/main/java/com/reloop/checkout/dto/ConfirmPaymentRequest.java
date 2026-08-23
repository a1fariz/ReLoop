package com.reloop.checkout.dto;

import java.util.UUID;

public class ConfirmPaymentRequest {
    private UUID reservationToken;
    private String paymentMethod;
    private String shippingAddress;

    public ConfirmPaymentRequest() {}

    public ConfirmPaymentRequest(UUID reservationToken, String paymentMethod, String shippingAddress) {
        this.reservationToken = reservationToken;
        this.paymentMethod = paymentMethod;
        this.shippingAddress = shippingAddress;
    }

    public UUID getReservationToken() { return reservationToken; }
    public void setReservationToken(UUID reservationToken) { this.reservationToken = reservationToken; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; }
}
