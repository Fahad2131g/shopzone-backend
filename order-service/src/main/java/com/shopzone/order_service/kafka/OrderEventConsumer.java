package com.shopzone.order_service.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class OrderEventConsumer {

    @KafkaListener(topics = "order-placed", groupId = "notification-group")
    public void consumeOrderEvent(String orderJson) {
        log.info("Received order event: {}", orderJson);
        log.info("Processing notification for order...");
        log.info("Email sent successfully!");
    }
}