package com.shopstream.order;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ElementCollection
    @CollectionTable(
            name = "order_items",
            joinColumns = @JoinColumn(name = "order_id")
    )
    @OrderColumn(name = "item_position")
    private List<OrderItem> items = new ArrayList<>();

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalPrice = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            columnDefinition = "varchar(20) default 'PENDING'"
    )
    private OrderStatus status = OrderStatus.PENDING;

    @Column(length = 500)
    private String rejectionReason;

    protected Order() {
    }

    public Order(List<OrderItem> items) {
        this.items = new ArrayList<>(items);

        for (OrderItem item : items) {
            BigDecimal itemTotal = item.getUnitPrice()
                    .multiply(BigDecimal.valueOf(item.getQuantity()));

            this.totalPrice = this.totalPrice.add(itemTotal);
        }
    }

    public boolean confirm() {
        if (status != OrderStatus.PENDING) {
            return false;
        }

        status = OrderStatus.CONFIRMED;
        rejectionReason = null;
        return true;
    }

    public boolean reject(String reason) {
        if (status != OrderStatus.PENDING) {
            return false;
        }

        status = OrderStatus.REJECTED;
        rejectionReason = reason;
        return true;
    }

    public Long getId() {
        return id;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }
}