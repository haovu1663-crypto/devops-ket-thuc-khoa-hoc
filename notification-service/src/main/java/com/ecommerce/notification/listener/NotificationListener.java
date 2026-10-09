package com.ecommerce.notification.listener;

import com.ecommerce.notification.config.RabbitConfig;
import com.ecommerce.notification.dto.NotificationEvent;
import com.ecommerce.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationListener {

    private final NotificationService notificationService;

    @RabbitListener(queues = {RabbitConfig.ORDER_QUEUE, RabbitConfig.PAYMENT_QUEUE})
    public void onEvent(NotificationEvent event) {
        log.info("Received notification event: type={}, orderId={}", event.getEventType(), event.getOrderId());
        notificationService.process(event);
    }
}
