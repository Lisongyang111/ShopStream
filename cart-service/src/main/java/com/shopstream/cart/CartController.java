package com.shopstream.cart;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/carts")
public class CartController {

    private final CartService cartService;
    private final CheckoutService checkoutService;

    public CartController(
            CartService cartService,
            CheckoutService checkoutService) {

        this.cartService = cartService;
        this.checkoutService = checkoutService;
    }

    @GetMapping("/{userId}")
    public Cart get(
            @PathVariable("userId") String userId) {

        return cartService.get(userId);
    }

    @PostMapping("/{userId}/items")
    public Cart add(
            @PathVariable("userId") String userId,
            @Valid @RequestBody CartItemRequest request) {

        return cartService.add(userId, request);
    }

    @PutMapping("/{userId}/items/{productId}")
    public Cart updateQuantity(
            @PathVariable("userId") String userId,
            @PathVariable("productId") Long productId,
            @Valid @RequestBody CartQuantityRequest request) {

        return cartService.updateQuantity(
                userId,
                productId,
                request.quantity()
        );
    }

    @DeleteMapping("/{userId}/items/{productId}")
    public Cart remove(
            @PathVariable("userId") String userId,
            @PathVariable("productId") Long productId) {

        return cartService.remove(userId, productId);
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clear(
            @PathVariable("userId") String userId) {

        cartService.clear(userId);
    }

    @PostMapping("/{userId}/checkout")
    public ResponseEntity<OrderResponse> checkout(
            @PathVariable("userId") String userId) {

        OrderResponse order = checkoutService.checkout(userId);

        if ("PENDING".equals(order.status())) {
            return ResponseEntity
                    .status(HttpStatus.ACCEPTED)
                    .body(order);
        }

        if ("REJECTED".equals(order.status())) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(order);
        }

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(order);
    }
}