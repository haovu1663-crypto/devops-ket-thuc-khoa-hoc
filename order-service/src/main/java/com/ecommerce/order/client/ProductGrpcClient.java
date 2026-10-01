package com.ecommerce.order.client;

import com.ecommerce.grpc.GetProductRequest;
import com.ecommerce.grpc.ProductGrpcResponse;
import com.ecommerce.grpc.ProductGrpcServiceGrpc;
import com.ecommerce.order.exception.BusinessException;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * gRPC Client — gọi sang product-service qua HTTP/2 + Protobuf.
 * Latency mục tiêu < 5ms (theo README).
 * Dùng tên "product-service" khớp với cấu hình grpc.client trong application.yml.
 */
@Component
public class ProductGrpcClient {

    private static final Logger log = LoggerFactory.getLogger(ProductGrpcClient.class);

    @GrpcClient("product-service")
    private ProductGrpcServiceGrpc.ProductGrpcServiceBlockingStub productStub;

    /**
     * Lấy thông tin sản phẩm từ product-service qua gRPC.
     * Dùng khi: (1) đặt hàng cần snapshot tên/giá, (2) khởi tạo/restock inventory.
     *
     * @param productId ID sản phẩm cần tra cứu
     * @return ProductGrpcResponse chứa id, name, price, stock, category
     * @throws BusinessException nếu sản phẩm không tồn tại hoặc service không phản hồi
     */
    public ProductGrpcResponse getProductById(Long productId) {
        try {
            log.debug("gRPC call → product-service: getProductById({})", productId);

            ProductGrpcResponse response = productStub.getProductById(
                    GetProductRequest.newBuilder()
                            .setProductId(productId)
                            .build()
            );

            log.debug("gRPC response: product '{}' price={}", response.getName(), response.getPrice());
            return response;

        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.NOT_FOUND) {
                throw new BusinessException("Sản phẩm ID=" + productId + " không tồn tại trong hệ thống");
            }
            log.error("gRPC call thất bại: {}", e.getMessage());
            throw new BusinessException("Không thể kết nối tới product-service: " + e.getStatus().getDescription());
        }
    }
}
