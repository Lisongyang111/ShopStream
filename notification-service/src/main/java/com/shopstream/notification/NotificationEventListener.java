package com.shopstream.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class NotificationEventListener {

    private static final Logger log =
            LoggerFactory.getLogger(NotificationEventListener.class);

    private final NotificationRecordRepository repository;

    public NotificationEventListener(
            NotificationRecordRepository repository) {
        this.repository = repository;
    }

    @Transactional
    @KafkaListener(
            topics = {"order-events", "inventory-events"},
            groupId = "notification-service"
    )
    public void handle(NotificationEvent event) {
        log.info(
                "Received {} for order {}, reason: {}",
                event.eventType(),
                event.orderId(),
                event.reason()
        );

        boolean confirmed =
                "OrderConfirmed".equals(event.eventType());

        boolean rejected =
                "OrderRejected".equals(event.eventType());

        if (!confirmed && !rejected) {
            return;
        }

        if (event.orderId() == null) {
            throw new IllegalArgumentException("Order ID is required");
        }

        if (repository.existsById(event.orderId())) {
            log.info(
                    "Skipped duplicate notification for order {}",
                    event.orderId()
            );
            return;
        }

        repository.saveAndFlush(new NotificationRecord(
                event.orderId(),
                event.eventType()
        ));

        String status = confirmed ? "CONFIRMED" : "REJECTED";

        log.info(
                "EMAIL: Order {} has been {}",
                event.orderId(),
                status
        );
    }
}