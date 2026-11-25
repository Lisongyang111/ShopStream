package com.shopstream.cart;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CartQuantityRequest(
        @NotNull @Positive Integer quantity
) {
}