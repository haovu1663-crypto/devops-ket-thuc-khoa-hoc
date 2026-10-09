package com.ecommerce.notification.repository;

import com.ecommerce.notification.entity.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {

    List<NotificationLog> findTop50ByOrderByCreatedAtDesc();

    List<NotificationLog> findByReferenceIdOrderByCreatedAtDesc(Long referenceId);
}
