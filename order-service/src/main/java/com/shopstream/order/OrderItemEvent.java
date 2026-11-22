package com.shopstream.order;

public record OrderItemEvent(
        Long productId,
        Integer quantity
) {
}