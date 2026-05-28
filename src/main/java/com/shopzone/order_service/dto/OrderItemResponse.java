package com.shopzone.order_service.dto;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderItemResponse {
    private Long id;
    private String productId;
    private String productName;
    private double price;
    private int quantity;
}