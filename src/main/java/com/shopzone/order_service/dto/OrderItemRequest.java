package com.shopzone.order_service.dto;

import lombok.Data;

@Data
public class OrderItemRequest {
    private String productId;
    private String productName;
    private double price;
    private int quantity;
}