package com.shopstream.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "notification_records")
public class NotificationRecord {

    @Id
    private Long orderId;

    @Column(nullable = false, length = 30)
    private String eventType;

    protected NotificationRecord() {
    }

    public NotificationRecord(Long orderId, String eventType) {
        this.orderId = orderId;
        this.eventType = eventType;
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getEventType() {
        return eventType;
    }
}