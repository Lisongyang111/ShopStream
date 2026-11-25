package com.shopstream.product;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {

    private final ProductRepository productRepository;
    private final InventoryReservationRepository reservationRepository;

    public InventoryService(
            ProductRepository productRepository,
            InventoryReservationRepository reservationRepository) {

        this.productRepository = productRepository;
        this.reservationRepository = reservationRepository;
    }

    @Transactional
    @CacheEvict(
            cacheNames = "products",
            allEntries = true,
            condition = "#result.eventType() == 'StockReserved'"
    )
    public InventoryEvent reserve(OrderEvent event) {
        if (event.orderId() == null) {
            throw new IllegalArgumentException("Order ID is required");
        }

        InventoryReservation previous = reservationRepository
                .findById(event.orderId())
                .orElse(null);

        if (previous != null) {
            return previous.toEvent();
        }

        if (event.items() == null || event.items().isEmpty()) {
            return reject(event.orderId(), "Order contains no items");
        }

        Map<Long, Long> quantities = new TreeMap<>();

        for (OrderItemEvent item : event.items()) {
            if (item == null
                    || item.productId() == null
                    || item.productId() <= 0
                    || item.quantity() == null
                    || item.quantity() <= 0) {

                return reject(
                        event.orderId(),
                        "Invalid product ID or quantity"
                );
            }

            quantities.merge(
                    item.productId(),
                    item.quantity().longValue(),
                    Math::addExact
            );
        }

        Map<Long, Product> products = new LinkedHashMap<>();

        for (Map.Entry<Long, Long> entry : quantities.entrySet()) {
            Product product = productRepository
                    .findByIdForUpdate(entry.getKey())
                    .orElse(null);

            if (product == null) {
                return reject(
                        event.orderId(),
                        "Product not found: " + entry.getKey()
                );
            }

            if (product.getStock().longValue() < entry.getValue()) {
                return reject(
                        event.orderId(),
                        "Insufficient stock for product: " + entry.getKey()
                );
            }

            products.put(entry.getKey(), product);
        }

        for (Map.Entry<Long, Long> entry : quantities.entrySet()) {
            Product product = products.get(entry.getKey());

            product.setStock(
                    product.getStock() - entry.getValue().intValue()
            );
        }

        InventoryReservation reservation = new InventoryReservation(
                event.orderId(),
                "StockReserved",
                null
        );

        reservationRepository.save(reservation);

        return reservation.toEvent();
    }

    private InventoryEvent reject(Long orderId, String reason) {
        InventoryReservation reservation = new InventoryReservation(
                orderId,
                "StockRejected",
                reason
        );

        reservationRepository.save(reservation);

        return reservation.toEvent();
    }
}