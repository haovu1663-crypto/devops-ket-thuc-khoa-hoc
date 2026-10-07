package com.ecommerce.payment.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceResponse {

    private Long id;
    private Long paymentId;
    private Long orderId;
    private String invoiceNumber;
    private String customerName;
    private String customerEmail;
    private BigDecimal totalAmount;
    private BigDecimal taxAmount;
    private LocalDateTime issuedAt;
}
