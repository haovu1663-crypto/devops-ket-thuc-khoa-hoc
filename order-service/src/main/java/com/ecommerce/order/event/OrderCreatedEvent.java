package com.ecommerce.order.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long orderId;
    private String userId;
    private Double totalPrice;
    private String paymentMethod;
    private String bankCode;
    private LocalDateTime createdAt;
}
