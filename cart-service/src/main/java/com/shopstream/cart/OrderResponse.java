package com.shopstream.cart;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderResponse(
        Long id,
        String status,
        String rejectionReason,
        BigDecimal totalPrice
) {
}