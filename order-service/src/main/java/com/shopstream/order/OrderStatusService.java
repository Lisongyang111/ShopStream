package com.shopstream.order;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderStatusService {

    private final OrderRepository repository;

    public OrderStatusService(OrderRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public OrderEvent apply(InventoryEvent event) {
        OrderStatus targetStatus;
        String finalEventType;

        if ("StockReserved".equals(event.eventType())) {
            targetStatus = OrderStatus.CONFIRMED;
            finalEventType = "OrderConfirmed";
        } else if ("StockRejected".equals(event.eventType())) {
            targetStatus = OrderStatus.REJECTED;
            finalEventType = "OrderRejected";
        } else {
            return null;
        }

        if (event.orderId() == null) {
            throw new IllegalArgumentException("Order ID is required");
        }

        Order order = repository
                .findByIdForUpdate(event.orderId())
                .orElseThrow(() -> new IllegalStateException(
                        "Order not found: " + event.orderId()
                ));

        if (order.getStatus() == OrderStatus.PENDING) {
            if (targetStatus == OrderStatus.CONFIRMED) {
                order.confirm();
            } else {
                order.reject(event.reason());
            }
        } else if (order.getStatus() != targetStatus) {
            return null;
        }

        List<OrderItemEvent> items = order.getItems()
                .stream()
                .map(item -> new OrderItemEvent(
                        item.getProductId(),
                        item.getQuantity()
                ))
                .toList();

        return new OrderEvent(
                finalEventType,
                order.getId(),
                items
        );
    }
}