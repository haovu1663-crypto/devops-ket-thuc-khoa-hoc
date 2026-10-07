package com.ecommerce.payment.controller;

import com.ecommerce.payment.dto.*;
import com.ecommerce.payment.service.PaymentService;
import com.ecommerce.payment.service.VNPayService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final VNPayService vnPayService;

    /**
     * Tạo yêu cầu thanh toán mới (VNPAY, COD...)
     */
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<PaymentResponse>> createPayment(
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "anonymous") String userId,
            @Valid @RequestBody CreatePaymentRequest request,
            HttpServletRequest httpRequest) {

        log.info("Create payment request from userId={}, orderId={}, method={}", userId, request.getOrderId(), request.getPaymentMethod());
        PaymentResponse response = paymentService.createPayment(userId, request, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Tạo yêu cầu thanh toán thành công", response));
    }

    /**
     * Tra cứu chi tiết giao dịch thanh toán theo ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentById(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "anonymous") String userId,
            @RequestHeader(value = "X-User-Role", required = false, defaultValue = "ROLE_USER") String role) {

        PaymentResponse response = paymentService.getPaymentById(id, userId, role);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin thanh toán thành công", response));
    }

    /**
     * Tra cứu thanh toán theo Order ID
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentByOrderId(
            @PathVariable Long orderId,
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "anonymous") String userId,
            @RequestHeader(value = "X-User-Role", required = false, defaultValue = "ROLE_USER") String role) {

        PaymentResponse response = paymentService.getPaymentByOrderId(orderId, userId, role);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin thanh toán theo đơn hàng thành công", response));
    }

    /**
     * Lịch sử thanh toán của user đang đăng nhập
     */
    @GetMapping("/my-payments")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getMyPayments(
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "anonymous") String userId) {

        List<PaymentResponse> response = paymentService.getMyPayments(userId);
        return ResponseEntity.ok(ApiResponse.success("Lấy lịch sử thanh toán thành công", response));
    }

    /**
     * Tra cứu hóa đơn điện tử của đơn hàng
     */
    @GetMapping("/invoices/{orderId}")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getInvoiceByOrderId(
            @PathVariable Long orderId,
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "anonymous") String userId,
            @RequestHeader(value = "X-User-Role", required = false, defaultValue = "ROLE_USER") String role) {

        InvoiceResponse response = paymentService.getInvoiceByOrderId(orderId, userId, role);
        return ResponseEntity.ok(ApiResponse.success("Lấy hóa đơn điện tử thành công", response));
    }

    /**
     * Return URL: VNPAY chuyển hướng người dùng về sau khi thanh toán
     */
    @GetMapping("/vnpay-return")
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleVNPayReturn(HttpServletRequest request) {
        Map<String, String> vnpParams = new HashMap<>();
        Map<String, String[]> requestParams = request.getParameterMap();

        for (Map.Entry<String, String[]> entry : requestParams.entrySet()) {
            if (entry.getValue() != null && entry.getValue().length > 0) {
                vnpParams.put(entry.getKey(), entry.getValue()[0]);
            }
        }

        // Xử lý tự động cập nhật trạng thái đơn hàng và sinh hóa đơn (Idempotent với Redis Lock)
        VNPayIpnResponse ipnResult = paymentService.processVNPayIpn(vnpParams);

        boolean isValid = vnPayService.verifySignature(vnpParams);
        String responseCode = vnpParams.get("vnp_ResponseCode");
        String txnRef = vnpParams.get("vnp_TxnRef");
        String amount = vnpParams.get("vnp_Amount");

        Map<String, Object> result = new HashMap<>();
        result.put("transactionCode", txnRef);
        result.put("amount", amount != null ? Long.parseLong(amount) / 100 : 0);
        result.put("isValidSignature", isValid);
        result.put("responseCode", responseCode);
        result.put("ipnResult", ipnResult.getMessage());

        if (isValid && "00".equals(responseCode)) {
            result.put("status", "SUCCESS");
            result.put("message", "Giao dịch thanh toán VNPAY thành công! Đơn hàng đã được xác nhận.");
            return ResponseEntity.ok(ApiResponse.success("Thanh toán VNPAY thành công", result));
        } else {
            result.put("status", "FAILED");
            result.put("message", "Giao dịch thanh toán VNPAY thất bại hoặc đã bị hủy (Mã: " + responseCode + ")");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(400, "Thanh toán không thành công. Mã lỗi: " + responseCode));
        }
    }
}
