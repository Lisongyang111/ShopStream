package com.shopstream.cart;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CartService {

    private final CartRepository repository;

    public CartService(CartRepository repository) {
        this.repository = repository;
    }

    public Cart get(String userId) {
        return repository.findById(userId)
                .orElseGet(() -> new Cart(userId));
    }

    public Cart add(String userId, CartItemRequest request) {
        Cart cart = get(userId);

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

        cart.getItems().removeIf(item ->
                item.getProductId().equals(productId)
        );

        return save(cart);
    }

    public void clear(String userId) {
        repository.deleteById(userId);
    }

    private Cart save(Cart cart) {
        cart.setTimeToLive(86400L);
        return repository.save(cart);
    }
}