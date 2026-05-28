package com.shopzone.product_service.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

@Document(collection = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
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