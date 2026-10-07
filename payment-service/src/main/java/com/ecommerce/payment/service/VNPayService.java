package com.ecommerce.payment.service;

import com.ecommerce.payment.config.VNPayConfig;
import com.ecommerce.payment.entity.Payment;
import com.ecommerce.payment.util.VNPayUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class VNPayService {

    private final VNPayConfig vnPayConfig;

    /**
     * Sinh URL thanh toán VNPAY Sandbox có kèm chữ ký bảo mật HMAC-SHA512 (từ HttpServletRequest)
     */
    public String createPaymentUrl(Payment payment, HttpServletRequest request) {
        String ipAddress = request != null ? VNPayUtil.getIpAddress(request) : "127.0.0.1";
        return createPaymentUrl(payment, ipAddress);
    }

    /**
     * Sinh URL thanh toán VNPAY Sandbox có kèm chữ ký bảo mật HMAC-SHA512 (từ IP cụ thể)
     */
    public String createPaymentUrl(Payment payment, String ipAddress) {
        long amountInVndUnits = payment.getAmount().longValue() * 100L; // VNPAY tính theo đơn vị x100 (Ví dụ: 10,000 VND -> 1000000)

        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_Version", vnPayConfig.getVersion());
        vnpParams.put("vnp_Command", vnPayConfig.getCommand());
        vnpParams.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        vnpParams.put("vnp_Amount", String.valueOf(amountInVndUnits));
        vnpParams.put("vnp_CurrCode", "VND");

        if (payment.getBankCode() != null && !payment.getBankCode().trim().isEmpty()) {
            vnpParams.put("vnp_BankCode", payment.getBankCode().trim());
        }

        vnpParams.put("vnp_TxnRef", payment.getTransactionCode());
        vnpParams.put("vnp_OrderInfo", "Thanh toan don hang #" + payment.getOrderId() + " - Ma GD: " + payment.getTransactionCode());
        vnpParams.put("vnp_OrderType", vnPayConfig.getOrderType());
        vnpParams.put("vnp_Locale", "vn");
        vnpParams.put("vnp_ReturnUrl", vnPayConfig.getReturnUrl());
        vnpParams.put("vnp_IpAddr", ipAddress != null && !ipAddress.isEmpty() ? ipAddress : "127.0.0.1");

        TimeZone vnTimeZone = TimeZone.getTimeZone("Asia/Ho_Chi_Minh");
        Calendar cld = Calendar.getInstance(vnTimeZone);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        formatter.setTimeZone(vnTimeZone);
        String vnpCreateDate = formatter.format(cld.getTime());
        vnpParams.put("vnp_CreateDate", vnpCreateDate);

        // Hết hạn sau 15 phút
        cld.add(Calendar.MINUTE, 15);
        String vnpExpireDate = formatter.format(cld.getTime());
        vnpParams.put("vnp_ExpireDate", vnpExpireDate);

        // Sắp xếp các tham số và build query string chuẩn xác
        List<String> fieldNames = new ArrayList<>(vnpParams.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();

        for (String fieldName : fieldNames) {
            String fieldValue = vnpParams.get(fieldName);
            if (fieldValue != null && !fieldValue.trim().isEmpty()) {
                if (hashData.length() > 0) {
                    hashData.append('&');
                    query.append('&');
                }
                hashData.append(fieldName).append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII)).append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
            }
        }

        String vnpSecureHash = VNPayUtil.hmacSHA512(vnPayConfig.getHashSecret(), hashData.toString());
        query.append("&vnp_SecureHash=").append(vnpSecureHash);

        String paymentUrl = vnPayConfig.getPayUrl() + "?" + query.toString();
        log.info("Generated VNPAY payment URL for transactionCode={}: {}", payment.getTransactionCode(), paymentUrl);
        return paymentUrl;
    }

    /**
     * Xác thực chữ ký số HMAC-SHA512 từ Webhook IPN hoặc Return Callback
     */
    public boolean verifySignature(Map<String, String> vnpParams) {
        String vnpSecureHash = vnpParams.get("vnp_SecureHash");
        if (vnpSecureHash == null || vnpSecureHash.isEmpty()) {
            return false;
        }

        // Loại bỏ các trường không tham gia ký
        Map<String, String> fields = new HashMap<>(vnpParams);
        fields.remove("vnp_SecureHashType");
        fields.remove("vnp_SecureHash");

        String signValue = VNPayUtil.hashAllFields(fields, vnPayConfig.getHashSecret());
        boolean isValid = signValue.equalsIgnoreCase(vnpSecureHash);
        log.debug("VNPay signature verification: calculated={}, received={}, valid={}", signValue, vnpSecureHash, isValid);
        return isValid;
    }
}
