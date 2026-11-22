package com.shopstream.product;

import java.util.List;

public record OrderEvent(
        String eventType,
        Long orderId,
        List<OrderItemEvent> items
) {
}