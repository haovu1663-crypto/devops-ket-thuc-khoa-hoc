package com.ecommerce.notification.controller;

import com.ecommerce.notification.entity.NotificationLog;
import com.ecommerce.notification.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationLogRepository logRepository;

    /** 50 thông báo gần nhất */
    @GetMapping
    public List<NotificationLog> latest() {
        return logRepository.findTop50ByOrderByCreatedAtDesc();
    }

    /** Lịch sử thông báo của một đơn hàng */
    @GetMapping("/order/{orderId}")
    public List<NotificationLog> byOrder(@PathVariable Long orderId) {
        return logRepository.findByReferenceIdOrderByCreatedAtDesc(orderId);
    }
}
