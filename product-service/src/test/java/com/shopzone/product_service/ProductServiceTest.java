package com.shopzone.product_service;

import com.shopzone.product_service.dto.ProductRequest;
import com.shopzone.product_service.dto.ProductResponse;
import com.shopzone.product_service.exception.ProductNotFoundException;
import com.shopzone.product_service.model.Product;
import com.shopzone.product_service.repository.ProductRepository;
import com.shopzone.product_service.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private com.shopzone.product_service.service.CloudinaryService cloudinaryService;

    @InjectMocks
    private ProductService productService;

    private Product testProduct;
    private ProductRequest productRequest;

    @BeforeEach
    void setUp() {
        testProduct = Product.builder()
                .id("abc123")
                .name("Nike Air Max")
                .description("Comfortable shoes")
                .price(129.99)
                .stock(50)
                .category("shoes")
                .createdBy("test@gmail.com")
                .build();

        productRequest = new ProductRequest();
        productRequest.setName("Nike Air Max");
        productRequest.setDescription("Comfortable shoes");
        productRequest.setPrice(129.99);
        productRequest.setStock(50);
        productRequest.setCategory("shoes");
    }

    @Test
    void shouldCreateProductSuccessfully() {
        when(productRepository.save(any())).thenReturn(testProduct);

        ProductResponse response = productService.createProduct(productRequest, "test@gmail.com");

        assertNotNull(response);
        assertEquals("Nike Air Max", response.getName());
        assertEquals(129.99, response.getPrice());
        verify(productRepository, times(1)).save(any());
    }

    @Test
    void shouldGetAllProductsSuccessfully() {
        when(productRepository.findAll()).thenReturn(List.of(testProduct));

        List<ProductResponse> responses = productService.getAllProducts();

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals("Nike Air Max", responses.get(0).getName());
    }

    @Test
    void shouldGetProductByIdSuccessfully() {
        when(productRepository.findById("abc123")).thenReturn(Optional.of(testProduct));

        ProductResponse response = productService.getProductById("abc123");

        assertNotNull(response);
        assertEquals("abc123", response.getId());
    }

    @Test
    void shouldThrowExceptionWhenProductNotFound() {
        when(productRepository.findById(any())).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> {
            productService.getProductById("nonexistent");
        });
    }

    @Test
    void shouldDeleteProductSuccessfully() {
        when(productRepository.findById("abc123")).thenReturn(Optional.of(testProduct));

        productService.deleteProduct("abc123");

        verify(productRepository, times(1)).delete(testProduct);
    }

    @Test
    void shouldSearchProductsByName() {
        when(productRepository.findByNameContainingIgnoreCase("nike"))
                .thenReturn(List.of(testProduct));

        List<ProductResponse> responses = productService.searchProducts("nike");

        assertNotNull(responses);
        assertEquals(1, responses.size());
    }
}