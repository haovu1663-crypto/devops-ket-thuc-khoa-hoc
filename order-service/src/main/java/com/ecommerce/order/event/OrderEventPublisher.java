package com.ecommerce.order.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventPublisher {

    public static final String ORDER_CREATED_TOPIC = "order.created.topic";

    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;
    private final RabbitTemplate rabbitTemplate;

    public void publishOrderCreated(OrderCreatedEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            RTopic topic = redissonClient.getTopic(ORDER_CREATED_TOPIC, StringCodec.INSTANCE);
            long receivers = topic.publish(json);
            log.info("Published OrderCreatedEvent for order #{} to topic '{}' (received by {} listeners): {}",
                    event.getOrderId(), ORDER_CREATED_TOPIC, receivers, json);
        } catch (Exception e) {
            log.error("Failed to publish OrderCreatedEvent for order #{}: {}", event.getOrderId(), e.getMessage(), e);
        }

        publishToNotificationQueue(event);
    }

    /** Gửi event sang notification-service qua RabbitMQ (bất đồng bộ, lỗi broker không ảnh hưởng đặt hàng). */
    private void publishToNotificationQueue(OrderCreatedEvent event) {
        try {
            NotificationEvent message = NotificationEvent.builder()
                    .eventType("ORDER_CREATED")
                    .orderId(event.getOrderId())
                    .userId(event.getUserId())
                    .customerEmail(event.getCustomerEmail())
                    .amount(event.getTotalPrice())
                    .paymentMethod(event.getPaymentMethod())
                    .bankCode(event.getBankCode())
                    .occurredAt(event.getCreatedAt())
                    .build();
            rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.ORDER_KEY, message);
            log.info("Published ORDER_CREATED notification for order #{} to RabbitMQ", event.getOrderId());
        } catch (Exception e) {
            log.error("Failed to publish notification for order #{} to RabbitMQ: {}", event.getOrderId(), e.getMessage());
        }
    }
}
