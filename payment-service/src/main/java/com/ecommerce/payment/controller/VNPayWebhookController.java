package com.ecommerce.payment.controller;

import com.ecommerce.payment.dto.VNPayIpnResponse;
import com.ecommerce.payment.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/payments/webhook")
@RequiredArgsConstructor
//nơi tiếp nhận kết quả thanh toán từ VNPAY
public class VNPayWebhookController {

    private final PaymentService paymentService;

    

    /**
     * IPN Webhook nhận kết quả từ VNPAY Server (hỗ trợ cả GET & POST)
     */
    @RequestMapping(value = "/vnpay", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<VNPayIpnResponse> handleVNPayIpn(HttpServletRequest request) {
        Map<String, String> vnpParams = new HashMap<>();
        Map<String, String[]> requestParams = request.getParameterMap();

        for (Map.Entry<String, String[]> entry : requestParams.entrySet()) {
            if (entry.getValue() != null && entry.getValue().length > 0) {
                vnpParams.put(entry.getKey(), entry.getValue()[0]);
            }
        }

        log.info("Received VNPay IPN request with {} parameters", vnpParams.size());
        VNPayIpnResponse ipnResponse = paymentService.processVNPayIpn(vnpParams);
        return ResponseEntity.ok(ipnResponse);
    }
}
