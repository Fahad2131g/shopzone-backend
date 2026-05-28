package com.shopzone.product_service.dto;

import lombok.*;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductResponse {
    private String id;
    private String name;
    private String description;
    private double price;
    private int stock;
    private String category;
    private String imageUrl;
    private List<String> images;
    private String createdBy;
}