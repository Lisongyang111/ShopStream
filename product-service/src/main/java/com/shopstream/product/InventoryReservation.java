package com.shopstream.product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "inventory_reservations")
public class InventoryReservation {

    @Id
    private Long orderId;

    @Column(nullable = false, length = 30)
    private String eventType;

    @Column(length = 500)
    private String reason;

    protected InventoryReservation() {
    }

    public InventoryReservation(
            Long orderId,
            String eventType,
            String reason) {

        this.orderId = orderId;
        this.eventType = eventType;
        this.reason = reason;
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getReason() {
        return reason;
    }

    public InventoryEvent toEvent() {
        return new InventoryEvent(
                eventType,
                orderId,
                reason
        );
    }
}