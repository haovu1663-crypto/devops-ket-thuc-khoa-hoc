package com.ecommerce.product.grpc;

import com.ecommerce.grpc.GetProductRequest;
import com.ecommerce.grpc.ProductGrpcResponse;
import com.ecommerce.grpc.ProductGrpcServiceGrpc;
import com.ecommerce.product.repository.ProductRepository;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC Server cho product-service.
 * Order-service sẽ gọi vào đây để lấy thông tin sản phẩm
 * với độ trễ thấp (Latency < 5ms) qua HTTP/2 + Protobuf.
 */
@GrpcService
public class ProductGrpcServiceImpl
        extends ProductGrpcServiceGrpc.ProductGrpcServiceImplBase {

    private final ProductRepository productRepository;

    public ProductGrpcServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public void getProductById(GetProductRequest request,
                               StreamObserver<ProductGrpcResponse> responseObserver) {

        productRepository.findById(request.getProductId())
                .ifPresentOrElse(
                        product -> {
                            // Build Protobuf response
                            ProductGrpcResponse response = ProductGrpcResponse.newBuilder()
                                    .setId(product.getId())
                                    .setName(product.getName())
                                    .setPrice(product.getPrice())
                                    .setStock(product.getStock())
                                    .setCategory(product.getCategory())
                                    .build();

                            responseObserver.onNext(response);
                            responseObserver.onCompleted();
                        },
                        () -> responseObserver.onError(
                                // Trả về NOT_FOUND nếu sản phẩm không tồn tại
                                Status.NOT_FOUND
                                        .withDescription("Không tìm thấy sản phẩm với ID: "
                                                + request.getProductId())
                                        .asRuntimeException()
                        )
                );
    }
}
