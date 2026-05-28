package com.shopzone.product_service.dto;

import lombok.Data;
import java.util.List;

@Data
public class ProductRequest {
    private String name;
    private String description;
    private double price;
    private int stock;
    private String category;
    private List<String> images;
}