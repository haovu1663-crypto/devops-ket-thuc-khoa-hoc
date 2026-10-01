package com.ecommerce.order.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "order_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @ToString.Exclude
    private Order order;

    /**
     * ID tham chiếu sang product-service (không hard FK vì khác DB).
     */
    @Column(name = "product_id", nullable = false)
    private Long productId;

    /**
     * SNAPSHOT: Tên sản phẩm tại thời điểm đặt hàng.
     * Nếu admin đổi tên SP sau này, lịch sử đơn hàng vẫn giữ tên cũ.
     */
    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;

    /**
     * SNAPSHOT: Giá tại thời điểm đặt hàng.
     */
    @Column(name = "unit_price", nullable = false)
    private Double unitPrice;

    @Column(nullable = false)
    private Integer quantity;

    /**
     * = unitPrice * quantity
     */
    @Column(nullable = false)
    private Double subtotal;
}
