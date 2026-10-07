package com.ecommerce.payment.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Slf4j
@Component
public class OrderServiceClient {

    private final RestClient restClient;

    public OrderServiceClient(@Value("${services.order-service.url:http://order-service:8083}") String orderServiceUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(orderServiceUrl)
                .build();
    }

    /**
     * Thông báo cho order-service chuyển trạng thái đơn hàng (ví dụ sang CONFIRMED hoặc COMPLETED)
     */
    public void updateOrderStatus(Long orderId, String newStatus) {
        try {
            log.info("Notifying order-service to update order #{} status to {}", orderId, newStatus);
            restClient.patch()
                    .uri("/api/v1/orders/{id}/status", orderId)
                    .header("X-User-Role", "ROLE_ADMIN")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("status", newStatus))
                    .retrieve()
                    .toBodilessEntity();
            log.info("Successfully updated order #{} status to {} in order-service", orderId, newStatus);
        } catch (Exception e) {
            log.error("Failed to update order #{} status in order-service: {}", orderId, e.getMessage());
        }
    }
}
