package com.shopstream.product;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderEventListener {

    private static final Logger log =
            LoggerFactory.getLogger(OrderEventListener.class);

    private final InventoryService inventoryService;
    private final KafkaTemplate<String, InventoryEvent> kafkaTemplate;

    public OrderEventListener(
            InventoryService inventoryService,
            KafkaTemplate<String, InventoryEvent> kafkaTemplate) {

        this.inventoryService = inventoryService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(
            topics = "order-events",
            groupId = "product-service"
    )
    public void handle(OrderEvent event) {
        if (!"OrderPlaced".equals(event.eventType())) {
            return;
        }

        log.info(
                "Received OrderPlaced for order {}",
                event.orderId()
        );

        InventoryEvent result = inventoryService.reserve(event);

        kafkaTemplate.send(
                "inventory-events",
                result.orderId().toString(),
                result
        ).join();

        log.info(
                "Published {} for order {}, reason: {}",
                result.eventType(),
                result.orderId(),
                result.reason()
        );
    }
}