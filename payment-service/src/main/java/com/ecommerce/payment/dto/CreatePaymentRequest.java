package com.ecommerce.payment.dto;

import com.ecommerce.payment.entity.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentRequest {

    @NotNull(message = "Mã đơn hàng (orderId) không được để trống")
    private Long orderId;

    @NotNull(message = "Số tiền thanh toán không được để trống")
    @DecimalMin(value = "1000.0", message = "Số tiền thanh toán tối thiểu là 1,000 VND")
    private BigDecimal amount;

    @NotNull(message = "Phương thức thanh toán không được để trống (VNPAY, COD, MOMO, BANK_TRANSFER)")
    private PaymentMethod paymentMethod;

    private String bankCode; // Tùy chọn: NCB, VCB, VISA... (dành cho VNPAY)

    private String customerName;

    private String customerEmail;
}
