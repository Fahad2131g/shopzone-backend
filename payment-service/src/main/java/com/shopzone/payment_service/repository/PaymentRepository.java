package com.shopzone.payment_service.repository;

import com.shopzone.payment_service.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByUserEmail(String userEmail);
    List<Payment> findByOrderId(Long orderId);
}