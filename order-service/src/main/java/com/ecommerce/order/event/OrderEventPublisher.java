package com.ecommerce.order.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventPublisher {

    public static final String ORDER_CREATED_TOPIC = "order.created.topic";

    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;

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
    }
}
