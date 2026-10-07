package com.ecommerce.order.dto;

import com.ecommerce.order.entity.OrderStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OrderResponse {
    private Long id;
    private String userId;
    private OrderStatus status;
    private Double totalPrice;
    private String paymentMethod;
    private String note;
    private List<OrderItemResponse> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
