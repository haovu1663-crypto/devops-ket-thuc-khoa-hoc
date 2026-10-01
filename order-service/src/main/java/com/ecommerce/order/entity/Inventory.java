package com.ecommerce.order.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventory")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ID tham chiếu sang product-service (unique, không hard FK).
     */
    @Column(name = "product_id", nullable = false, unique = true)
    private Long productId;

    /**
     * Tên sản phẩm — lưu từ gRPC call khi khởi tạo inventory.
     * Dùng để dễ tra cứu, không cần gọi product-service mỗi lần.
     */
    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;

    /**
     * Tổng số hàng thực tế nhập vào kho.
     */
    @Column(name = "total_stock", nullable = false)
    @Builder.Default
    private Integer totalStock = 0;

    /**
     * Số hàng đang bị "giữ" bởi các đơn hàng PENDING/CONFIRMED.
     * Tăng khi đặt hàng → Giảm khi hủy đơn hoặc giao hàng xong.
     */
    @Column(name = "reserved_stock", nullable = false)
    @Builder.Default
    private Integer reservedStock = 0;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Số hàng có thể mua thêm = totalStock - reservedStock.
     * Đây là con số kiểm tra khi khách đặt hàng.
     */
    @Transient
    public int getAvailableStock() {
        return totalStock - reservedStock;
    }
}
