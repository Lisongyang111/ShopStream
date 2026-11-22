package com.shopstream.notification;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRecordRepository
        extends JpaRepository<NotificationRecord, Long> {
}