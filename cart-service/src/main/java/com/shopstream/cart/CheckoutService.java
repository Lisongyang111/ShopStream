package com.shopstream.cart;

import java.util.List;

import feign.FeignException;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CheckoutService {

    private final CartService cartService;
    private final OrderClient orderClient;

    public CheckoutService(
            CartService cartService,
            OrderClient orderClient) {

        this.cartService = cartService;
        this.orderClient = orderClient;
    }

    public OrderResponse checkout(String userId) {
        Cart cart = cartService.getForCheckout(userId);

        if (cart.getItems().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cart is empty"
            );
        }

        try {
            OrderResponse order;

            if (cart.getCheckoutOrderId() != null) {
                order = orderClient.findById(
                        cart.getCheckoutOrderId()
                );
            } else {
                order = orderClient.create(
                        List.copyOf(cart.getItems())
                );

                cartService.markCheckout(cart, order.id());
            }

            for (int attempt = 0;
                    attempt < 10 && "PENDING".equals(order.status());
                    attempt++) {

                Thread.sleep(300);
                order = orderClient.findById(order.id());
            }

            cartService.applyCheckoutResult(cart, order);

            return order;

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Checkout was interrupted",
                    exception
            );

        } catch (FeignException exception) {
            if (exception.status() >= 400
                    && exception.status() < 500) {

                throw new ResponseStatusException(
                        HttpStatusCode.valueOf(exception.status()),
                        "Order service rejected the checkout request",
                        exception
                );
            }

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Could not communicate with order service",
                    exception
            );
        }
    }
}