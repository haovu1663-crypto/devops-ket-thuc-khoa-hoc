package com.ecommerce.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

/**
 * Cấu hình KeyResolver cho cơ chế RequestRateLimiter tại API Gateway.
 * Sử dụng Redis để lưu trạng thái Token Bucket.
 */
@Configuration
public class RateLimiterConfig {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterConfig.class);

    /**
     * KeyResolver theo Địa chỉ IP của Client (dành cho API công khai: Login, Register, Public Catalog).
     * Ưu tiên đọc từ Header X-Forwarded-For (khi chạy sau Nginx reverse proxy),
     * nếu không có thì lấy trực tiếp từ Remote Address.
     */
    @Primary
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String forwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
            String clientIp;

            if (forwardedFor != null && !forwardedFor.isBlank()) {
                // Lấy IP đầu tiên trong danh sách forward (IP gốc của client)
                clientIp = forwardedFor.split(",")[0].trim();
            } else {
                InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
                clientIp = (remoteAddress != null && remoteAddress.getAddress() != null)
                        ? remoteAddress.getAddress().getHostAddress()
                        : "unknown-ip";
            }

            log.trace("RateLimiter IP Key: {}", clientIp);
            return Mono.just("ip:" + clientIp);
        };
    }

    /**
     * KeyResolver theo Định danh người dùng (dành cho API yêu cầu đăng nhập: Order, Payment).
     * Nếu có header X-User-Id (được inject sau khi verify JWT) thì dùng userId làm khóa,
     * nếu không có thì tự động fallback về IP.
     */
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            if (userId != null && !userId.isBlank()) {
                return Mono.just("user:" + userId);
            }

            // Fallback về IP nếu chưa có X-User-Id
            InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
            String fallbackIp = (remoteAddress != null && remoteAddress.getAddress() != null)
                    ? remoteAddress.getAddress().getHostAddress()
                    : "anonymous";
            return Mono.just("ip:" + fallbackIp);
        };
    }
}
