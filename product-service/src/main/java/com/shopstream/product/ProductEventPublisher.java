package com.shopstream.product;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ProductEventPublisher {

    private static final Logger log =
            LoggerFactory.getLogger(ProductEventPublisher.class);

    private final KafkaTemplate<String, ProductEvent> kafkaTemplate;

    public ProductEventPublisher(
            KafkaTemplate<String, ProductEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(ProductEvent event) {
        kafkaTemplate.send(
                "product-events",
                event.productId().toString(),
                event
        ).join();

        log.info(
                "Published {} for product {}",
                event.eventType(),
                event.productId()
        );
    }
}