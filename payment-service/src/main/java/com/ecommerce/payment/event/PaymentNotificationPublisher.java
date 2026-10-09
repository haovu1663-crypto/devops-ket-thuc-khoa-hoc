package com.ecommerce.payment.event;

import com.ecommerce.payment.entity.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentNotificationPublisher {

    private final RabbitTemplate rabbitTemplate;

    /** Gửi PAYMENT_SUCCESS sang notification-service. Lỗi broker không làm hỏng luồng thanh toán. */
    public void publishPaymentSuccess(Payment payment) {
        try {
            NotificationEvent message = NotificationEvent.builder()
                    .eventType("PAYMENT_SUCCESS")
                    .orderId(payment.getOrderId())
                    .userId(payment.getUserId())
                    .customerEmail(payment.getUserId() != null && payment.getUserId().contains("@") ? payment.getUserId() : null)
                    .amount(payment.getAmount() != null ? payment.getAmount().doubleValue() : null)
                    .paymentMethod(payment.getPaymentMethod() != null ? payment.getPaymentMethod().name() : null)
                    .bankCode(payment.getBankCode())
                    .transactionCode(payment.getTransactionCode())
                    .providerTxnRef(payment.getProviderTxnRef())
                    .occurredAt(payment.getPaymentTime() != null ? payment.getPaymentTime() : LocalDateTime.now())
                    .build();
            rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.PAYMENT_KEY, message);
            log.info("Published PAYMENT_SUCCESS notification for order #{} to RabbitMQ", payment.getOrderId());
        } catch (Exception e) {
            log.error("Failed to publish PAYMENT_SUCCESS notification for order #{}: {}",
                    payment.getOrderId(), e.getMessage());
        }
    }
}
