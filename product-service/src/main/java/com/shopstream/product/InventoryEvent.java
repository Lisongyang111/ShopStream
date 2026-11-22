package com.shopstream.product;

public record InventoryEvent(
        String eventType,
        Long orderId,
        String reason
) {
}