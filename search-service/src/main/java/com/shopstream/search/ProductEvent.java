package com.shopstream.search;

import java.math.BigDecimal;

public record ProductEvent(
        String eventType,
        Long productId,
        String name,
        String description,
        String category,
        BigDecimal price,
        Integer stock
) {
}