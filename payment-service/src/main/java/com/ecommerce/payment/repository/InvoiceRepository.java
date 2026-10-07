package com.ecommerce.payment.repository;

import com.ecommerce.payment.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findByOrderId(Long orderId);

    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);
}
