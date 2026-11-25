package com.shopstream.cart;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
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
        Cart cart = cartService.get(userId);

        if (cart.getItems().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cart is empty"
            );
        }

        try {
            OrderResponse order = orderClient.create(
                    List.copyOf(cart.getItems())
            );

            for (int attempt = 0;
                    attempt < 10 && "PENDING".equals(order.status());
                    attempt++) {

                Thread.sleep(300);
                order = orderClient.findById(order.id());
            }

            if ("CONFIRMED".equals(order.status())) {
                cartService.clear(userId);
            } else if (!"REJECTED".equals(order.status())
                    && !"PENDING".equals(order.status())) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Unknown order status"
                );
            }

            return order;

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Checkout was interrupted",
                    exception
            );

        } catch (HttpClientErrorException exception) {
            throw new ResponseStatusException(
                    exception.getStatusCode(),
                    "Order service rejected the checkout request",
                    exception
            );

        } catch (RestClientException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Could not communicate with order service",
                    exception
            );
        }
    }
}