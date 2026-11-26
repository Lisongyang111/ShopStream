package com.shopstream.cart;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class OrderClient {

    private final OrderFeignClient client;

    public OrderClient(OrderFeignClient client) {
        this.client = client;
    }

    public OrderResponse create(List<CartItem> items) {
        return requireResponse(client.create(items));
    }

    public OrderResponse findById(Long orderId) {
        return requireResponse(client.findById(orderId));
    }

    private OrderResponse requireResponse(OrderResponse response) {
        if (response == null
                || response.id() == null
                || response.status() == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Invalid response from order service"
            );
        }

        return response;
    }
}