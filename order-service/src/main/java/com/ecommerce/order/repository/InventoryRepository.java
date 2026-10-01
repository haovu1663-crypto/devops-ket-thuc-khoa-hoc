package com.ecommerce.order.repository;

import com.ecommerce.order.entity.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByProductId(Long productId);

    boolean existsByProductId(Long productId);

    /**
     * SELECT FOR UPDATE — Pessimistic Lock chống Race Condition.
     * Khi nhiều request cùng mua 1 sản phẩm (Flash Sale),
     * chỉ 1 transaction được lock row tại 1 thời điểm.
     * Các transaction khác phải chờ lock được giải phóng.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.productId = :productId")
    Optional<Inventory> findByProductIdWithLock(@Param("productId") Long productId);
}
