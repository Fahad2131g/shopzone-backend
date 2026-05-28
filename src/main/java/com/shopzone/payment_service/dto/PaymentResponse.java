package com.shopzone.payment_service.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentResponse {
    private Long id;
    private Long orderId;
    private String userEmail;
    private double amount;
    private String status;
    private String transactionId;
    private LocalDateTime createdAt;
}