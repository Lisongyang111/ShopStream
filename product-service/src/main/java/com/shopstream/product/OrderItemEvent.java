package com.shopstream.product;

public record OrderItemEvent(
        Long productId,
        Integer quantity
) {
}