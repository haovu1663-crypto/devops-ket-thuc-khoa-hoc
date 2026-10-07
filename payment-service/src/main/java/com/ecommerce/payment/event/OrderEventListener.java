package com.ecommerce.payment.event;

import com.ecommerce.payment.entity.Payment;
import com.ecommerce.payment.entity.PaymentLog;
import com.ecommerce.payment.entity.PaymentMethod;
import com.ecommerce.payment.entity.PaymentStatus;
import com.ecommerce.payment.repository.PaymentLogRepository;
import com.ecommerce.payment.repository.PaymentRepository;
import com.ecommerce.payment.service.VNPayService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {

    public static final String ORDER_CREATED_TOPIC = "order.created.topic";

    private final RedissonClient redissonClient;
    private final PaymentRepository paymentRepository;
    private final PaymentLogRepository paymentLogRepository;
    private final VNPayService vnPayService;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void initListener() {
        RTopic topic = redissonClient.getTopic(ORDER_CREATED_TOPIC, StringCodec.INSTANCE);
        topic.addListener(String.class, (channel, jsonMessage) -> {
            try {
                log.info("Received raw JSON event on topic '{}': {}", channel, jsonMessage);
                OrderCreatedEvent event = objectMapper.readValue(jsonMessage, OrderCreatedEvent.class);
                handleOrderCreated(event);
            } catch (Exception e) {
                log.error("Error processing OrderCreatedEvent message: {}", jsonMessage, e);
            }
        });
        log.info("Successfully subscribed to topic '{}' for OrderCreatedEvent", ORDER_CREATED_TOPIC);
    }

    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent: orderId={}, amount={}, method={}, userId={}",
                event.getOrderId(), event.getTotalPrice(), event.getPaymentMethod(), event.getUserId());

        // Kiểm tra Idempotency: Đã có giao dịch cho orderId này chưa
        if (paymentRepository.findFirstByOrderIdOrderByCreatedAtDesc(event.getOrderId()).isPresent()) {
            log.info("Payment already exists for order #{}, skipping duplicate event", event.getOrderId());
            return;
        }

        PaymentMethod method = PaymentMethod.COD;
        try {
            if (event.getPaymentMethod() != null) {
                method = PaymentMethod.valueOf(event.getPaymentMethod().toUpperCase());
            }
        } catch (IllegalArgumentException e) {
            log.warn("Unknown paymentMethod '{}', defaulting to COD", event.getPaymentMethod());
        }

        String txnCode = "TXN" + System.currentTimeMillis() + (int) (Math.random() * 9000 + 1000);
        Payment payment = Payment.builder()
                .orderId(event.getOrderId())
                .userId(event.getUserId())
                .amount(BigDecimal.valueOf(event.getTotalPrice()))
                .paymentMethod(method)
                .status(PaymentStatus.PENDING)
                .transactionCode(txnCode)
                .bankCode(event.getBankCode())
                .build();

        if (method == PaymentMethod.VNPAY) {
            String paymentUrl = vnPayService.createPaymentUrl(payment, "127.0.0.1");
            payment.setPaymentUrl(paymentUrl);
        }

        Payment savedPayment = paymentRepository.save(payment);

        paymentLogRepository.save(PaymentLog.builder()
                .paymentId(savedPayment.getId())
                .eventType("EVENT_PAYMENT_INIT")
                .payload("Created via OrderCreatedEvent: " + event)
                .isValidSignature(true)
                .build());

        log.info("Successfully created payment #{} (txn={}) via event for order #{}",
                savedPayment.getId(), txnCode, event.getOrderId());
    }
}
