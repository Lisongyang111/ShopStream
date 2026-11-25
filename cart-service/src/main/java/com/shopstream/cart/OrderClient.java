package com.shopstream.cart;

import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@Component
public class OrderClient {

    private final RestClient restClient;

    public OrderClient(
            RestClient.Builder builder,
            @Value("${order.service.url}") String baseUrl) {

        SimpleClientHttpRequestFactory factory =
                new SimpleClientHttpRequestFactory();

        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(5));

        this.restClient = builder
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }

    public OrderResponse create(List<CartItem> items) {
        OrderResponse response = restClient.post()
                .uri("/orders")
                .body(items)
                .retrieve()
                .body(OrderResponse.class);

        return requireResponse(response);
    }

    public OrderResponse findById(Long orderId) {
        OrderResponse response = restClient.get()
                .uri("/orders/{id}", orderId)
                .retrieve()
                .body(OrderResponse.class);

        return requireResponse(response);
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