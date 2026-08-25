package com.shopzone.payment_service;

import com.shopzone.payment_service.dto.PaymentRequest;
import com.shopzone.payment_service.dto.PaymentResponse;
import com.shopzone.payment_service.exception.PaymentNotFoundException;
import com.shopzone.payment_service.model.Payment;
import com.shopzone.payment_service.model.PaymentStatus;
import com.shopzone.payment_service.repository.PaymentRepository;
import com.shopzone.payment_service.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentService paymentService;

    private Payment testPayment;
    private PaymentRequest paymentRequest;

    @BeforeEach
    void setUp() {
        testPayment = Payment.builder()
                .id(1L)
                .orderId(1L)
                .userEmail("test@gmail.com")
                .amount(259.98)
                .status(PaymentStatus.SUCCESS)
                .transactionId(UUID.randomUUID().toString())
                .createdAt(LocalDateTime.now())
                .build();

        paymentRequest = new PaymentRequest();
        paymentRequest.setOrderId(1L);
        paymentRequest.setAmount(259.98);
    }

    @Test
    void shouldProcessPaymentSuccessfully() {
        when(paymentRepository.save(any())).thenReturn(testPayment);

        PaymentResponse response = paymentService.processPayment(paymentRequest, "test@gmail.com");

        assertNotNull(response);
        assertEquals("test@gmail.com", response.getUserEmail());
        assertEquals(259.98, response.getAmount());
        verify(paymentRepository, times(1)).save(any());
    }

    @Test
    void shouldReturnValidTransactionId() {
        when(paymentRepository.save(any())).thenReturn(testPayment);

        PaymentResponse response = paymentService.processPayment(paymentRequest, "test@gmail.com");

        assertNotNull(response.getTransactionId());
    }

    @Test
    void shouldGetPaymentByIdSuccessfully() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));

        PaymentResponse response = paymentService.getPaymentById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
    }

    @Test
    void shouldThrowExceptionWhenPaymentNotFound() {
        when(paymentRepository.findById(any())).thenReturn(Optional.empty());

        assertThrows(PaymentNotFoundException.class, () -> {
            paymentService.getPaymentById(999L);
        });
    }

    @Test
    void shouldGetMyPaymentsSuccessfully() {
        when(paymentRepository.findByUserEmail("test@gmail.com"))
                .thenReturn(List.of(testPayment));

        List<PaymentResponse> responses = paymentService.getMyPayments("test@gmail.com");

        assertNotNull(responses);
        assertEquals(1, responses.size());
    }

    @Test
    void shouldGetAllPaymentsSuccessfully() {
        when(paymentRepository.findAll()).thenReturn(List.of(testPayment));

        List<PaymentResponse> responses = paymentService.getAllPayments();

        assertNotNull(responses);
        assertEquals(1, responses.size());
    }
}