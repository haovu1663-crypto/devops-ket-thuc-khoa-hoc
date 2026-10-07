package com.ecommerce.payment.dto;

import com.ecommerce.payment.entity.PaymentMethod;
import com.ecommerce.payment.entity.PaymentStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    private Long id;
    private Long orderId;
    private String userId;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private PaymentStatus status;
    private String transactionCode;
    private String providerTxnRef;
    private String bankCode;
    private String paymentUrl;
    private String failureReason;
    private LocalDateTime paymentTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
