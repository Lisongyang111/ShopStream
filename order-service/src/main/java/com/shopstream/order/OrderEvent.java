package com.shopstream.order;

import java.util.List;

public record OrderEvent(
        String eventType,
        Long orderId,
        List<OrderItemEvent> items
) {
}