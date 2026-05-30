package com.shopzone.order_service;

import com.shopzone.order_service.dto.OrderItemRequest;
import com.shopzone.order_service.dto.OrderRequest;
import com.shopzone.order_service.dto.OrderResponse;
import com.shopzone.order_service.exception.OrderNotFoundException;
import com.shopzone.order_service.model.Order;
import com.shopzone.order_service.model.OrderItem;
import com.shopzone.order_service.model.OrderStatus;
import com.shopzone.order_service.repository.OrderRepository;
import com.shopzone.order_service.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    private Order testOrder;
    private OrderRequest orderRequest;

    @BeforeEach
    void setUp() {
        OrderItem testItem = OrderItem.builder()
                .id(1L)
                .productId("abc123")
                .productName("Nike Air Max")
                .price(129.99)
                .quantity(2)
                .build();

        testOrder = Order.builder()
                .id(1L)
                .userEmail("test@gmail.com")
                .totalAmount(259.98)
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .items(List.of(testItem))
                .build();

        OrderItemRequest itemRequest = new OrderItemRequest();
        itemRequest.setProductId("abc123");
        itemRequest.setProductName("Nike Air Max");
        itemRequest.setPrice(129.99);
        itemRequest.setQuantity(2);

        orderRequest = new OrderRequest();
        orderRequest.setItems(List.of(itemRequest));
    }

    @Test
    void shouldCreateOrderSuccessfully() {
        when(orderRepository.save(any())).thenReturn(testOrder);

        OrderResponse response = orderService.createOrder(orderRequest, "test@gmail.com");

        assertNotNull(response);
        assertEquals("test@gmail.com", response.getUserEmail());
        verify(orderRepository, times(1)).save(any());
    }

    @Test
    void shouldCalculateTotalCorrectly() {
        when(orderRepository.save(any())).thenReturn(testOrder);

        OrderResponse response = orderService.createOrder(orderRequest, "test@gmail.com");

        assertNotNull(response);
        assertEquals(259.98, response.getTotalAmount());
    }

    @Test
    void shouldGetOrderByIdSuccessfully() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        OrderResponse response = orderService.getOrderById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
    }

    @Test
    void shouldThrowExceptionWhenOrderNotFound() {
        when(orderRepository.findById(any())).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> {
            orderService.getOrderById(999L);
        });
    }

    @Test
    void shouldGetMyOrdersSuccessfully() {
        when(orderRepository.findByUserEmail("test@gmail.com"))
                .thenReturn(List.of(testOrder));

        List<OrderResponse> responses = orderService.getMyOrders("test@gmail.com");

        assertNotNull(responses);
        assertEquals(1, responses.size());
    }

    @Test
    void shouldUpdateOrderStatusSuccessfully() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any())).thenReturn(testOrder);

        OrderResponse response = orderService.updateOrderStatus(1L, "CONFIRMED");

        assertNotNull(response);
        verify(orderRepository, times(1)).save(any());
    }
}