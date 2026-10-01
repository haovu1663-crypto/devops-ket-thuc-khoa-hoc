package com.ecommerce.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InventoryRequest {

    @NotNull(message = "Product ID không được để trống")
    private Long productId;

    /**
     * Số lượng nhập thêm (tùy chọn).
     * Nếu không truyền, hệ thống tự động lấy từ stock của product-service qua gRPC.
     */
    @Min(value = 1, message = "Số lượng nhập thêm phải ít nhất là 1")
    private Integer quantity;

    /** Tương thích ngược nếu client gửi totalStock */
    @Min(value = 0, message = "Số lượng tồn kho không được âm")
    private Integer totalStock;
}
