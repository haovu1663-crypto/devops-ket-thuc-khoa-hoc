package com.ecommerce.payment.service;

import com.ecommerce.payment.client.OrderServiceClient;
import com.ecommerce.payment.dto.*;
import com.ecommerce.payment.entity.*;
import com.ecommerce.payment.exception.*;
import com.ecommerce.payment.repository.InvoiceRepository;
import com.ecommerce.payment.repository.PaymentLogRepository;
import com.ecommerce.payment.repository.PaymentRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentLogRepository paymentLogRepository;
    private final InvoiceRepository invoiceRepository;
    private final VNPayService vnPayService;
    private final OrderServiceClient orderServiceClient;
    private final RedissonClient redissonClient;
    private final org.springframework.transaction.support.TransactionTemplate transactionTemplate;
    private final com.ecommerce.payment.event.PaymentNotificationPublisher notificationPublisher;

    /**
     * Tạo yêu cầu thanh toán (VNPAY, COD...)
     */
    @Transactional
    public PaymentResponse createPayment(String userId, CreatePaymentRequest request, HttpServletRequest httpRequest) {
        // tạo mã giao dịch duy nhất
        String txnCode = "TXN" + System.currentTimeMillis() + (int) (Math.random() * 9000 + 1000);

        Payment payment = Payment.builder()
                .orderId(request.getOrderId())
                .userId(userId)
                .amount(request.getAmount())
                .paymentMethod(request.getPaymentMethod())
                .status(PaymentStatus.PENDING)
                .transactionCode(txnCode)
                .bankCode(request.getBankCode())
                .build();

        // Xử lý riêng theo từng phương thức thanh toán 
        if (request.getPaymentMethod() == PaymentMethod.VNPAY) {
            String paymentUrl = vnPayService.createPaymentUrl(payment, httpRequest);
            payment.setPaymentUrl(paymentUrl);
        } else if (request.getPaymentMethod() == PaymentMethod.COD) {
            log.info("Creating COD payment for order #{} by user {}", request.getOrderId(), userId);
        }

        Payment savedPayment = paymentRepository.save(payment);

        // Lưu log khởi tạo
        paymentLogRepository.save(PaymentLog.builder()
                .paymentId(savedPayment.getId())
                .eventType("PAYMENT_INIT")
                .payload("Method: " + request.getPaymentMethod() + " | Amount: " + request.getAmount())
                .isValidSignature(true)
                .build());

        log.info("Created payment #{} (txn={}) for order #{}", savedPayment.getId(), txnCode, request.getOrderId());
        return mapToResponse(savedPayment);
    }

    /**
     * Xử lý Webhook IPN từ VNPAY (với Idempotency & Distributed Lock)
     */
    public VNPayIpnResponse processVNPayIpn(Map<String, String> vnpParams) {
        // Ghi log toàn bộ tham số nhận được từ VNPAY để phục vụ truy vết và đối soát giao dịch
        log.info("Received VNPay IPN webhook: {}", vnpParams);

        // BƯỚC 1: Kiểm tra chữ ký số HMAC-SHA512
        // Hash lại toàn bộ tham số bằng Secret Key và so sánh với vnp_SecureHash từ VNPAY gửi sang
        boolean isValidSignature = vnPayService.verifySignature(vnpParams);
        // Lấy mã tham chiếu giao dịch nội bộ của hệ thống (ví dụ: TXN172828...)
        String txnRef = vnpParams.get("vnp_TxnRef");

        // Nếu chữ ký không khớp: Dấu hiệu dữ liệu bị chỉnh sửa trên đường truyền hoặc request giả mạo
        if (!isValidSignature) {
            log.warn("VNPay IPN signature verification failed for txnRef={}", txnRef);
            // Ghi nhật ký lỗi vào bảng payment_logs (isSuccess = false)
            saveLog(null, "IPN_INVALID_SIGNATURE", vnpParams.toString(), vnpParams.get("vnp_SecureHash"), false);
            // Trả về mã lỗi 97 (Chữ ký không hợp lệ) theo chuẩn VNPAY
            return new VNPayIpnResponse("97", "Invalid Checksum");
        }

        // Kiểm tra mã giao dịch có tồn tại trong tham số gửi về hay không
        if (txnRef == null || txnRef.isEmpty()) {
            return new VNPayIpnResponse("01", "Order not found");
        }

        // BƯỚC 2: Sử dụng Distributed Lock (Redis Lock) chống xử lý trùng lặp đồng thời (Race Condition)
        // Đặt key khóa duy nhất gắn liền với mã giao dịch txnRef
        String lockKey = "lock:payment:txn:" + txnRef;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // Thử lấy khóa trong tối đa 3 giây (waitTime); nếu có khóa thì giữ tối đa 5 giây (leaseTime)
            // Ngăn chặn đồng thời Webhook IPN và Return URL xử lý cùng 1 đơn hàng tại cùng thời điểm
            boolean acquired = lock.tryLock(3, 5, TimeUnit.SECONDS);
            if (!acquired) {
                // Nếu luồng khác đang xử lý thì thoát ngay, tránh xung đột tài nguyên
                log.warn("Another IPN thread is currently processing txnRef={}", txnRef);
                return new VNPayIpnResponse("99", "Transaction is being processed");
            }

            // Mở Database Transaction để thực hiện xử lý nghiệp vụ bên trong (hỗ trợ Pessimistic Lock)
            return transactionTemplate.execute(status -> doProcessVNPayIpnInternal(vnpParams, txnRef));

        } catch (InterruptedException e) {
            // Đặt lại cờ ngắt của thread nếu bị ngắt tiến trình đột ngột
            Thread.currentThread().interrupt();
            return new VNPayIpnResponse("99", "System error: Interrupted");
        } finally {
            // Luôn giải phóng khóa Redis khi hoàn tất (chỉ luồng đang giữ khóa mới được unlock)
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Transactional
    protected VNPayIpnResponse doProcessVNPayIpnInternal(Map<String, String> vnpParams, String txnRef) {
        // BƯỚC 3: Tìm bản ghi thanh toán và áp dụng Khóa bi quan CSDL (SELECT ... FOR UPDATE)
        Optional<Payment> paymentOpt = paymentRepository.findByTransactionCodeWithLock(txnRef);
        if (paymentOpt.isEmpty()) {
            log.warn("VNPay IPN: Payment not found for txnRef={}", txnRef);
            saveLog(null, "IPN_NOT_FOUND", vnpParams.toString(), vnpParams.get("vnp_SecureHash"), true);
            // Mã 01: Không tìm thấy đơn hàng trong hệ thống
            return new VNPayIpnResponse("01", "Order not found");
        }

        Payment payment = paymentOpt.get();

        // BƯỚC 4: Kiểm tra tính toàn vẹn số tiền thanh toán (Amount Tampering Check)
        // VNPAY quy ước số tiền gửi về nhân 100 lần (ví dụ: 100.000 VNĐ -> gửi 10000000)
        long vnpAmount = Long.parseLong(vnpParams.get("vnp_Amount"));
        long expectedAmount = payment.getAmount().longValue() * 100L;
        if (vnpAmount != expectedAmount) {
            log.warn("VNPay IPN: Amount mismatch for txnRef={}, expected={}, received={}", txnRef, expectedAmount, vnpAmount);
            saveLog(payment.getId(), "IPN_AMOUNT_MISMATCH", vnpParams.toString(), vnpParams.get("vnp_SecureHash"), true);
            // Mã 04: Số tiền thanh toán không khớp với dữ liệu gốc
            return new VNPayIpnResponse("04", "Invalid Amount");
        }

        // BƯỚC 5: Kiểm tra Idempotency - Giao dịch này đã được xử lý xong trước đó chưa?
        if (payment.getStatus() == PaymentStatus.SUCCESS || payment.getStatus() == PaymentStatus.FAILED) {
            log.info("VNPay IPN: Transaction {} already processed with status {}", txnRef, payment.getStatus());
            // Mã 02: Giao dịch đã được cập nhật trạng thái trước đó, không xử lý lại
            return new VNPayIpnResponse("02", "Order already confirmed");
        }

        // BƯỚC 6: Trích xuất các mã đối soát từ phản hồi của cổng thanh toán VNPAY
        String responseCode = vnpParams.get("vnp_ResponseCode"); // Mã kết quả: "00" = thành công
        String providerTxnNo = vnpParams.get("vnp_TransactionNo"); // Mã giao dịch do VNPAY sinh ra
        String bankCode = vnpParams.get("vnp_BankCode");           // Mã ngân hàng thực hiện (NCB, VCB,...)

        // Lưu thông tin đối soát của ngân hàng vào bản ghi Payment
        payment.setProviderTxnRef(providerTxnNo);
        payment.setBankCode(bankCode);

        // BƯỚC 7: Cập nhật trạng thái thanh toán và đồng bộ các dịch vụ liên quan
        if ("00".equals(responseCode)) {
            // === TRƯỜNG HỢP: THANH TOÁN THÀNH CÔNG ===
            // Cập nhật trạng thái giao dịch sang SUCCESS và lưu thời điểm thanh toán
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setPaymentTime(LocalDateTime.now());
            paymentRepository.save(payment);

            // Tự động khởi tạo hóa đơn điện tử (Invoice) cho đơn hàng
            generateInvoice(payment);

            // Gọi REST API (OpenFeign) sang order-service để cập nhật đơn hàng thành CONFIRMED
            orderServiceClient.updateOrderStatus(payment.getOrderId(), "CONFIRMED");

            // Gửi thông báo thanh toán thành công sang notification-service (RabbitMQ, bất đồng bộ)
            notificationPublisher.publishPaymentSuccess(payment);

            log.info("VNPay IPN SUCCESS: Order #{} marked as CONFIRMED for txnRef={}", payment.getOrderId(), txnRef);
            // Ghi nhật ký thành công vào payment_logs
            saveLog(payment.getId(), "IPN_SUCCESS", vnpParams.toString(), vnpParams.get("vnp_SecureHash"), true);
            // Trả về mã 00: Báo cho VNPAY biết hệ thống đã xác nhận thành công
            return new VNPayIpnResponse("00", "Confirm Success");
        } else {
            // === TRƯỜNG HỢP: THANH TOÁN THẤT BẠI HOẶC NGƯỜI DÙNG HỦY ===
            // Cập nhật trạng thái giao dịch sang FAILED và lưu mã lỗi
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Mã lỗi VNPAY: " + responseCode);
            paymentRepository.save(payment);

            log.warn("VNPay IPN FAILED: txnRef={}, responseCode={}", txnRef, responseCode);
            // Ghi nhật ký thất bại vào payment_logs
            saveLog(payment.getId(), "IPN_FAILED", vnpParams.toString(), vnpParams.get("vnp_SecureHash"), true);
            // Vẫn trả về mã 00 để VNPAY biết kết quả đã được ghi nhận và không gửi lại webhook nữa
            return new VNPayIpnResponse("00", "Confirm Success");
        }
    }

    /**
     * Tra cứu chi tiết thanh toán theo ID
     */
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(Long id, String userId, String role) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giao dịch thanh toán #" + id));

        if (!"ROLE_ADMIN".equals(role) && !payment.getUserId().equals(userId)) {
            throw new AccessDeniedException("Bạn không có quyền xem thông tin thanh toán này");
        }
        return mapToResponse(payment);
    }

    /**
     * Tra cứu thanh toán theo Order ID
     */
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByOrderId(Long orderId, String userId, String role) {
        Payment payment = paymentRepository.findFirstByOrderIdOrderByCreatedAtDesc(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Chưa có giao dịch thanh toán cho đơn hàng #" + orderId));

        if (!"ROLE_ADMIN".equals(role) && !payment.getUserId().equals(userId)) {
            throw new AccessDeniedException("Bạn không có quyền xem thông tin thanh toán của đơn hàng này");
        }
        return mapToResponse(payment);
    }

    /**
     * Lịch sử thanh toán của user
     */
    @Transactional(readOnly = true)
    public List<PaymentResponse> getMyPayments(String userId) {
        return paymentRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    /**
     * Tra cứu hóa đơn của đơn hàng
     */
    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceByOrderId(Long orderId, String userId, String role) {
        Invoice invoice = invoiceRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Chưa có hóa đơn cho đơn hàng #" + orderId));

        Payment payment = paymentRepository.findById(invoice.getPaymentId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giao dịch thanh toán của hóa đơn"));

        if (!"ROLE_ADMIN".equals(role) && !payment.getUserId().equals(userId)) {
            throw new AccessDeniedException("Bạn không có quyền xem hóa đơn này");
        }

        return InvoiceResponse.builder()
                .id(invoice.getId())
                .paymentId(invoice.getPaymentId())
                .orderId(invoice.getOrderId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .customerName(invoice.getCustomerName())
                .customerEmail(invoice.getCustomerEmail())
                .totalAmount(invoice.getTotalAmount())
                .taxAmount(invoice.getTaxAmount())
                .issuedAt(invoice.getIssuedAt())
                .build();
    }

    // ========== Private helpers ==========

    private void generateInvoice(Payment payment) {
        String invNo = "INV-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-" + payment.getOrderId();
        BigDecimal tax = payment.getAmount().multiply(BigDecimal.valueOf(0.08)).setScale(2, RoundingMode.HALF_UP); // Thuế VAT 8%

        Invoice invoice = Invoice.builder()
                .paymentId(payment.getId())
                .orderId(payment.getOrderId())
                .invoiceNumber(invNo)
                .customerName("Khách hàng #" + payment.getUserId())
                .totalAmount(payment.getAmount())
                .taxAmount(tax)
                .build();

        invoiceRepository.save(invoice);
        log.info("Generated invoice {} for payment #{}", invNo, payment.getId());
    }

    private void saveLog(Long paymentId, String eventType, String payload, String signature, boolean isValid) {
        paymentLogRepository.save(PaymentLog.builder()
                .paymentId(paymentId)
                .eventType(eventType)
                .payload(payload)
                .signature(signature)
                .isValidSignature(isValid)
                .build());
    }

    private PaymentResponse mapToResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .orderId(payment.getOrderId())
                .userId(payment.getUserId())
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .status(payment.getStatus())
                .transactionCode(payment.getTransactionCode())
                .providerTxnRef(payment.getProviderTxnRef())
                .bankCode(payment.getBankCode())
                .paymentUrl(payment.getPaymentUrl())
                .failureReason(payment.getFailureReason())
                .paymentTime(payment.getPaymentTime())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}
