package com.shopstream.notification;

public record NotificationEvent(
        String eventType,
        Long orderId,
        String reason
) {
}