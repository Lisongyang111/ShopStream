package com.shopstream.order;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class OrderEventPublisher {

    private static final Logger log =
            LoggerFactory.getLogger(OrderEventPublisher.class);

    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;
    private final String orderEventsTopic;

    public OrderEventPublisher(
            KafkaTemplate<String, OrderEvent> kafkaTemplate,
            @Value("${app.topics.order-events}") String orderEventsTopic) {

        this.kafkaTemplate = kafkaTemplate;
        this.orderEventsTopic = orderEventsTopic;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(OrderEvent event) {
        kafkaTemplate.send(
                orderEventsTopic,
                event.orderId().toString(),
                event
        ).join();

        log.info(
                "Published {} for order {}",
                event.eventType(),
                event.orderId()
        );
    }
}