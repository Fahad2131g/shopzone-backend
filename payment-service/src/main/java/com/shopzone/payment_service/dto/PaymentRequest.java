package com.shopzone.payment_service.dto;

import lombok.Data;

@Data
public class PaymentRequest {
    private Long orderId;
    private double amount;
}