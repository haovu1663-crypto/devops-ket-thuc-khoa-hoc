package com.ecommerce.order.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Message gửi sang notification-service qua RabbitMQ. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {
    private String eventType;
    private Long orderId;
    private String userId;
    private String customerEmail;
    private Double amount;
    private String paymentMethod;
    private String bankCode;
    private String transactionCode;
    private String providerTxnRef;
    private LocalDateTime occurredAt;
}
