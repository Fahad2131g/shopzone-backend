package com.shopzone.payment_service.service;

import com.shopzone.payment_service.dto.PaymentRequest;
import com.shopzone.payment_service.dto.PaymentResponse;
import com.shopzone.payment_service.exception.PaymentNotFoundException;
import com.shopzone.payment_service.model.Payment;
import com.shopzone.payment_service.model.PaymentStatus;
import com.shopzone.payment_service.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public PaymentResponse processPayment(PaymentRequest request, String userEmail) {
        // simulate payment processing
        boolean paymentSuccess = simulatePayment(request.getAmount());

        Payment payment = Payment.builder()
                .orderId(request.getOrderId())
                .userEmail(userEmail)
                .amount(request.getAmount())
                .status(paymentSuccess ? PaymentStatus.SUCCESS : PaymentStatus.FAILED)
                .transactionId(UUID.randomUUID().toString())
                .createdAt(LocalDateTime.now())
                .build();

        paymentRepository.save(payment);
        return mapToResponse(payment);
    }

    public List<PaymentResponse> getMyPayments(String userEmail) {
        return paymentRepository.findByUserEmail(userEmail)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public PaymentResponse getPaymentById(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found with id: " + id));
        return mapToResponse(payment);
    }

    public List<PaymentResponse> getAllPayments() {
        return paymentRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // simulates payment — 90% success rate
    private boolean simulatePayment(double amount) {
        return Math.random() > 0.1;
    }

    private PaymentResponse mapToResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .orderId(payment.getOrderId())
                .userEmail(payment.getUserEmail())
                .amount(payment.getAmount())
                .status(payment.getStatus().name())
                .transactionId(payment.getTransactionId())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}