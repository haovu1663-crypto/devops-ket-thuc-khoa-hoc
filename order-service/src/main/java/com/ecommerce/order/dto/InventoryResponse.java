package com.ecommerce.order.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class InventoryResponse {
    private Long productId;
    private String productName;
    private Integer totalStock;
    private Integer reservedStock;
    private Integer availableStock;
    private LocalDateTime updatedAt;
}
