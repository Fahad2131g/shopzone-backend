package com.shopzone.order_service.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.shopzone.order_service.dto.OrderResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class OrderEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public OrderEventProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Publishes an order event to Kafka. This is deliberately fail-safe:
     * if Kafka is unreachable (e.g. not deployed in this environment),
     * the failure is only logged — it never propagates back to the caller,
     * so order creation always succeeds regardless of Kafka's availability.
     */
    public void publishOrderEvent(OrderResponse order) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            String orderJson = mapper.writeValueAsString(order);

            log.info("Publishing order event for order id: {}", order.getId());

            kafkaTemplate.send("order-placed", orderJson)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("Order event published successfully for order id: {}", order.getId());
                        } else {
                            log.warn("Could not publish order event for order id {} — Kafka may be unavailable in this environment. Order was still created successfully. Reason: {}",
                                    order.getId(), ex.getMessage());
                        }
                    });
        } catch (Exception e) {
            log.warn("Failed to serialize order event for order id {} — skipping event publish. Order was still created successfully. Reason: {}",
                    order.getId(), e.getMessage());
        }
    }
}