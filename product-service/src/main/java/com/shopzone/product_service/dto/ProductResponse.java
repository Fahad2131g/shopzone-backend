package com.shopzone.product_service.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class ProductResponse {
    private String id;
    private String name;
    private String description;
    private double price;
    private int stock;
    private String category;
    private String imageUrl;
    private List<String> images;
    private List<String> sizes;
    private boolean featured;
    private boolean newArrival;
    private String createdBy;
    private boolean bestSeller;
}