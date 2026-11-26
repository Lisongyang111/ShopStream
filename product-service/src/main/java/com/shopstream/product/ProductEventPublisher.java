package com.shopstream.product;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ProductEventPublisher {

    private static final Logger log =
            LoggerFactory.getLogger(ProductEventPublisher.class);

    private final KafkaTemplate<String, ProductEvent> kafkaTemplate;
    private final String productEventsTopic;

    public ProductEventPublisher(
            KafkaTemplate<String, ProductEvent> kafkaTemplate,
            @Value("${app.topics.product-events}") String productEventsTopic) {

        this.kafkaTemplate = kafkaTemplate;
        this.productEventsTopic = productEventsTopic;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(ProductEvent event) {
        kafkaTemplate.send(
                productEventsTopic,
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