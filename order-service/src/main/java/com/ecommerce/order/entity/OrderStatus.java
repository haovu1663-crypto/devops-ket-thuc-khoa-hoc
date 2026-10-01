package com.ecommerce.order.entity;

/**
 * Vòng đời đơn hàng:
 * PENDING → CONFIRMED → SHIPPING → COMPLETED
 *                    ↘ CANCELLED (từ PENDING hoặc CONFIRMED)
 */
public enum OrderStatus {
    PENDING,    // Vừa đặt, chờ xác nhận
    CONFIRMED,  // Admin đã xác nhận
    SHIPPING,   // Đang giao hàng
    COMPLETED,  // Giao hàng thành công
    CANCELLED   // Đã hủy
}
