package com.ecommerce.payment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "vnpay")
@Data
public class VNPayConfig {

    private String tmnCode;
    private String hashSecret;
    private String payUrl;
    private String returnUrl;
    private String version = "2.1.0";
    private String command = "pay";
    private String orderType = "other";
}
