package com.ecommerce.payment.entity;

public enum PaymentStatus {
    PENDING,        // Chờ thanh toán
    PROCESSING,     // Đang xử lý tại cổng thanh toán
    SUCCESS,        // Thanh toán thành công
    FAILED,         // Thanh toán thất bại
    CANCELLED,      // Giao dịch đã hủy
    REFUNDED        // Đã hoàn tiền
}
