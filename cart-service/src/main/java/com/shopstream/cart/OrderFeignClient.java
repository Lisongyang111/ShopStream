package com.shopstream.cart;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "order-service")
public interface OrderFeignClient {

    @PostMapping("/orders")
    OrderResponse create(@RequestBody List<CartItem> items);

    @GetMapping("/orders/{id}")
    OrderResponse findById(@PathVariable("id") Long orderId);
}