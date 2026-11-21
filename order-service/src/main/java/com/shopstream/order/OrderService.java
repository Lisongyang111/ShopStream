package com.shopstream.order;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository repository;
    private final ProductClient productClient;

    public OrderService(
            OrderRepository repository,
            ProductClient productClient) {

        this.repository = repository;
        this.productClient = productClient;
    }

    @Transactional
    public Order create(List<OrderItemRequest> requests) {
        List<OrderItem> items = new ArrayList<>();

        for (OrderItemRequest request : requests) {
            ProductResponse product =
                    productClient.findById(request.productId());

            OrderItem item = new OrderItem(
                    product.id(),
                    request.quantity(),
                    product.price()
            );

            items.add(item);
        }

        Order order = new Order(items);
        return repository.save(order);
    }

    public Order findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Order not found"
                ));
    }
}