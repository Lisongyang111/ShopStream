package com.shopstream.order;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository repository;
    private final ProductClient productClient;
    private final ApplicationEventPublisher eventPublisher;

    public OrderService(
            OrderRepository repository,
            ProductClient productClient,
            ApplicationEventPublisher eventPublisher) {

        this.repository = repository;
        this.productClient = productClient;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Order create(List<OrderItemRequest> requests) {
        List<OrderItem> items = new ArrayList<>();
        List<OrderItemEvent> eventItems = new ArrayList<>();

        for (OrderItemRequest request : requests) {
            ProductResponse product =
                    productClient.findById(request.productId());

            OrderItem item = new OrderItem(
                    product.id(),
                    request.quantity(),
                    product.price()
            );

            items.add(item);

            eventItems.add(new OrderItemEvent(
                    product.id(),
                    request.quantity()
            ));
        }

        Order order = repository.save(new Order(items));

        OrderEvent event = new OrderEvent(
                "OrderPlaced",
                order.getId(),
                List.copyOf(eventItems)
        );

        eventPublisher.publishEvent(event);

        return order;
    }

    public Order findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Order not found"
                ));
    }
}