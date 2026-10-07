package com.ecommerce.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class OrderRequest {

    @NotEmpty(message = "Đơn hàng phải có ít nhất 1 sản phẩm")
    @Valid
    private List<OrderItemRequest> items;

    @NotBlank(message = "Phương thức thanh toán không được để trống (VNPAY, COD...)")
    private String paymentMethod; // VNPAY, COD

    private String bankCode; // NCB, VCB, VISA... (dành cho VNPAY)

    private String note;
}

