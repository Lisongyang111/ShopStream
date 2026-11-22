package com.shopstream.order;

public record InventoryEvent(
        String eventType,
        Long orderId,
        String reason
) {
}