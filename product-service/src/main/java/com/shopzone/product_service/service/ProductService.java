package com.shopzone.product_service.service;

import com.shopzone.product_service.dto.ProductRequest;
import com.shopzone.product_service.dto.ProductResponse;
import com.shopzone.product_service.exception.ProductNotFoundException;
import com.shopzone.product_service.model.Product;
import com.shopzone.product_service.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CloudinaryService cloudinaryService;

    public ProductResponse createProduct(ProductRequest request, String createdBy) {
        // Set primary imageUrl from the first image in list if available
        String mainImageUrl = (request.getImages() != null && !request.getImages().isEmpty())
                ? request.getImages().get(0)
                : null;

        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stock(request.getStock())
                .category(request.getCategory())
                .imageUrl(mainImageUrl)
                .images(request.getImages())
                .sizes(request.getSizes())
                .featured(request.isFeatured())
                .newArrival(request.isNewArrival())
                .bestSeller(request.isBestSeller())
                .createdBy(createdBy)
                .build();

        Product savedProduct = productRepository.save(product);
        return mapToResponse(savedProduct);
    }

    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public ProductResponse getProductById(String id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + id));
        return mapToResponse(product);
    }

    public List<ProductResponse> getProductsByCategory(String category) {
        return productRepository.findByCategory(category)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<ProductResponse> searchProducts(String name) {
        return productRepository.findByNameContainingIgnoreCase(name)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public ProductResponse updateProduct(String id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + id));

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setCategory(request.getCategory());
        product.setImages(request.getImages());

        // Update main imageUrl if images list has entries
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            product.setImageUrl(request.getImages().get(0));
        }

        product.setSizes(request.getSizes());
        product.setFeatured(request.isFeatured());
        product.setNewArrival(request.isNewArrival());
        product.setBestSeller(request.isBestSeller());

        Product updatedProduct = productRepository.save(product);
        return mapToResponse(updatedProduct);
    }

    public void deleteProduct(String id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + id));
        productRepository.delete(product);
    }

    private ProductResponse mapToResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stock(product.getStock())
                .category(product.getCategory())
                .imageUrl(product.getImageUrl())
                .images(product.getImages())
                .sizes(product.getSizes())
                .featured(product.isFeatured())
                .newArrival(product.isNewArrival())
                .bestSeller(product.isBestSeller())
                .createdBy(product.getCreatedBy())
                .build();
    }

    public ProductResponse uploadProductImage(String productId, MultipartFile file) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + productId));

        String imageUrl = cloudinaryService.uploadImage(file);
        product.setImageUrl(imageUrl);

        // Also add to images array if not present
        if (product.getImages() != null && !product.getImages().contains(imageUrl)) {
            product.getImages().add(imageUrl);
        }

        Product savedProduct = productRepository.save(product);
        return mapToResponse(savedProduct);
    }
}