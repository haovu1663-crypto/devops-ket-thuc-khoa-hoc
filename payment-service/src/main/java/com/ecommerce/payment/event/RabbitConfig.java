package com.ecommerce.payment.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Khai báo topology RabbitMQ (giống notification-service) để queue luôn tồn tại
 * và giữ message khi notification-service đang tắt.
 */
@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "ecommerce.notification.exchange";
    public static final String DLX = "ecommerce.notification.dlx";
    public static final String ORDER_QUEUE = "order.notification.queue";
    public static final String PAYMENT_QUEUE = "payment.notification.queue";
    public static final String DLQ = "notification.dlq";
    public static final String ORDER_KEY = "order.created";
    public static final String PAYMENT_KEY = "payment.success";
    public static final String DEAD_KEY = "notification.dead";

    @Bean
    public TopicExchange notificationExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX, true, false);
    }

    @Bean
    public Queue orderNotificationQueue() {
        return QueueBuilder.durable(ORDER_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", DEAD_KEY)
                .build();
    }

    @Bean
    public Queue paymentNotificationQueue() {
        return QueueBuilder.durable(PAYMENT_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", DEAD_KEY)
                .build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DLQ).build();
    }

    @Bean
    public Binding orderNotificationBinding() {
        return BindingBuilder.bind(orderNotificationQueue()).to(notificationExchange()).with(ORDER_KEY);
    }

    @Bean
    public Binding paymentNotificationBinding() {
        return BindingBuilder.bind(paymentNotificationQueue()).to(notificationExchange()).with(PAYMENT_KEY);
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange()).with(DEAD_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
