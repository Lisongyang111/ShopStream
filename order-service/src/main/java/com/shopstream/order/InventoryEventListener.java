package com.shopstream.order;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class InventoryEventListener {

    private static final Logger log =
            LoggerFactory.getLogger(InventoryEventListener.class);

    private final OrderStatusService orderStatusService;
    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    public InventoryEventListener(
            OrderStatusService orderStatusService,
            KafkaTemplate<String, OrderEvent> kafkaTemplate) {

        this.orderStatusService = orderStatusService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(
            topics = "inventory-events",
            groupId = "order-service"
    )
    public void handle(InventoryEvent event) {
        log.info(
                "Received {} for order {}",
                event.eventType(),
                event.orderId()
        );

        OrderEvent result = orderStatusService.apply(event);

        if (result == null) {
            return;
        }

        kafkaTemplate.send(
                "order-events",
                result.orderId().toString(),
                result
        ).join();

        log.info(
                "Published {} for order {}",
                result.eventType(),
                result.orderId()
        );
    }
}