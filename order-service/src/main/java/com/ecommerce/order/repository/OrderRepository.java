package com.ecommerce.order.repository;

import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /** Lịch sử đơn hàng của 1 user, sắp xếp mới nhất trước */
    List<Order> findByUserIdOrderByCreatedAtDesc(String userId);

    /** Admin xem đơn theo trạng thái */
    List<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status);
}
