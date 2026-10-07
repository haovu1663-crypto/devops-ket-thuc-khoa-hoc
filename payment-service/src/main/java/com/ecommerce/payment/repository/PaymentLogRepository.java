package com.ecommerce.payment.repository;

import com.ecommerce.payment.entity.PaymentLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentLogRepository extends JpaRepository<PaymentLog, Long> {

    List<PaymentLog> findByPaymentIdOrderByCreatedAtDesc(Long paymentId);
}
