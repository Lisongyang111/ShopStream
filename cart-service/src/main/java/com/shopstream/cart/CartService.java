package com.shopstream.cart;

import feign.FeignException;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CartService {

    private final CartRepository repository;
    private final OrderClient orderClient;

    public CartService(
            CartRepository repository,
            OrderClient orderClient) {

        this.repository = repository;
        this.orderClient = orderClient;
    }

    public Cart getForCheckout(String userId) {
        return repository.findById(userId)
                .orElseGet(() -> new Cart(userId));
    }

    public Cart get(String userId) {
        Cart cart = getForCheckout(userId);

        if (cart.getCheckoutOrderId() == null) {
            return cart;
        }

        try {
            OrderResponse order = orderClient.findById(
                    cart.getCheckoutOrderId()
            );

            applyCheckoutResult(cart, order);

            if ("CONFIRMED".equals(order.status())) {
                return new Cart(userId);
            }

            return cart;

        } catch (FeignException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Could not check the checkout order",
                    exception
            );
        }
    }

    public void markCheckout(Cart cart, Long orderId) {
        cart.setCheckoutOrderId(orderId);
        save(cart);
    }

    public void applyCheckoutResult(
            Cart cart,
            OrderResponse order) {

        switch (order.status()) {
            case "CONFIRMED" ->
                    repository.deleteById(cart.getUserId());

            case "REJECTED" -> {
                cart.setCheckoutOrderId(null);
                save(cart);
            }

            case "PENDING" -> {
                // Keep the cart and its order ID for the next check.
            }

            default -> throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Unknown order status"
            );
        }
    }

    public Cart add(String userId, CartItemRequest request) {
        Cart cart = get(userId);
        requireEditable(cart);

        CartItem existing = cart.getItems()
                .stream()
                .filter(item -> item.getProductId()
                        .equals(request.productId()))
                .findFirst()
                .orElse(null);

        if (existing == null) {
            cart.getItems().add(new CartItem(
                    request.productId(),
                    request.quantity()
            ));
        } else {
            long quantity = (long) existing.getQuantity()
                    + request.quantity();

            if (quantity > Integer.MAX_VALUE) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Quantity is too large"
                );
            }

            existing.setQuantity((int) quantity);
        }

        return save(cart);
    }

    public Cart updateQuantity(
            String userId,
            Long productId,
            Integer quantity) {

        Cart cart = get(userId);
        requireEditable(cart);

        CartItem item = cart.getItems()
                .stream()
                .filter(current -> current.getProductId()
                        .equals(productId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Product is not in the cart"
                ));

        item.setQuantity(quantity);

        return save(cart);
    }

    public Cart remove(String userId, Long productId) {
        Cart cart = get(userId);
        requireEditable(cart);

        cart.getItems().removeIf(item ->
                item.getProductId().equals(productId)
        );

        return save(cart);
    }

    public void clear(String userId) {
        Cart cart = get(userId);
        requireEditable(cart);

        repository.deleteById(userId);
    }

    private void requireEditable(Cart cart) {
        if (cart.getCheckoutOrderId() != null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Checkout is pending; check the cart again later"
            );
        }
    }

    private Cart save(Cart cart) {
        cart.setTimeToLive(86400L);
        return repository.save(cart);
    }
}