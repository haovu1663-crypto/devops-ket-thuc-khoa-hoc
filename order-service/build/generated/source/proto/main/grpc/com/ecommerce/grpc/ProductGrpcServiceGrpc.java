package com.ecommerce.grpc;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.63.0)",
    comments = "Source: product_service.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class ProductGrpcServiceGrpc {

  private ProductGrpcServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "com.ecommerce.grpc.ProductGrpcService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.ecommerce.grpc.GetProductRequest,
      com.ecommerce.grpc.ProductGrpcResponse> getGetProductByIdMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetProductById",
      requestType = com.ecommerce.grpc.GetProductRequest.class,
      responseType = com.ecommerce.grpc.ProductGrpcResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.ecommerce.grpc.GetProductRequest,
      com.ecommerce.grpc.ProductGrpcResponse> getGetProductByIdMethod() {
    io.grpc.MethodDescriptor<com.ecommerce.grpc.GetProductRequest, com.ecommerce.grpc.ProductGrpcResponse> getGetProductByIdMethod;
    if ((getGetProductByIdMethod = ProductGrpcServiceGrpc.getGetProductByIdMethod) == null) {
      synchronized (ProductGrpcServiceGrpc.class) {
        if ((getGetProductByIdMethod = ProductGrpcServiceGrpc.getGetProductByIdMethod) == null) {
          ProductGrpcServiceGrpc.getGetProductByIdMethod = getGetProductByIdMethod =
              io.grpc.MethodDescriptor.<com.ecommerce.grpc.GetProductRequest, com.ecommerce.grpc.ProductGrpcResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetProductById"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.ecommerce.grpc.GetProductRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.ecommerce.grpc.ProductGrpcResponse.getDefaultInstance()))
              .setSchemaDescriptor(new ProductGrpcServiceMethodDescriptorSupplier("GetProductById"))
              .build();
        }
      }
    }
    return getGetProductByIdMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static ProductGrpcServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ProductGrpcServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ProductGrpcServiceStub>() {
        @java.lang.Override
        public ProductGrpcServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ProductGrpcServiceStub(channel, callOptions);
        }
      };
    return ProductGrpcServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static ProductGrpcServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ProductGrpcServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ProductGrpcServiceBlockingStub>() {
        @java.lang.Override
        public ProductGrpcServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ProductGrpcServiceBlockingStub(channel, callOptions);
        }
      };
    return ProductGrpcServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static ProductGrpcServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ProductGrpcServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ProductGrpcServiceFutureStub>() {
        @java.lang.Override
        public ProductGrpcServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ProductGrpcServiceFutureStub(channel, callOptions);
        }
      };
    return ProductGrpcServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public interface AsyncService {

    /**
     */
    default void getProductById(com.ecommerce.grpc.GetProductRequest request,
        io.grpc.stub.StreamObserver<com.ecommerce.grpc.ProductGrpcResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetProductByIdMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service ProductGrpcService.
   */
  public static abstract class ProductGrpcServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return ProductGrpcServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service ProductGrpcService.
   */
  public static final class ProductGrpcServiceStub
      extends io.grpc.stub.AbstractAsyncStub<ProductGrpcServiceStub> {
    private ProductGrpcServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ProductGrpcServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ProductGrpcServiceStub(channel, callOptions);
    }

    /**
     */
    public void getProductById(com.ecommerce.grpc.GetProductRequest request,
        io.grpc.stub.StreamObserver<com.ecommerce.grpc.ProductGrpcResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetProductByIdMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service ProductGrpcService.
   */
  public static final class ProductGrpcServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<ProductGrpcServiceBlockingStub> {
    private ProductGrpcServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ProductGrpcServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ProductGrpcServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public com.ecommerce.grpc.ProductGrpcResponse getProductById(com.ecommerce.grpc.GetProductRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetProductByIdMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service ProductGrpcService.
   */
  public static final class ProductGrpcServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<ProductGrpcServiceFutureStub> {
    private ProductGrpcServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ProductGrpcServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ProductGrpcServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.ecommerce.grpc.ProductGrpcResponse> getProductById(
        com.ecommerce.grpc.GetProductRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetProductByIdMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_GET_PRODUCT_BY_ID = 0;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final AsyncService serviceImpl;
    private final int methodId;

    MethodHandlers(AsyncService serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_GET_PRODUCT_BY_ID:
          serviceImpl.getProductById((com.ecommerce.grpc.GetProductRequest) request,
              (io.grpc.stub.StreamObserver<com.ecommerce.grpc.ProductGrpcResponse>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  public static final io.grpc.ServerServiceDefinition bindService(AsyncService service) {
    return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
        .addMethod(
          getGetProductByIdMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.ecommerce.grpc.GetProductRequest,
              com.ecommerce.grpc.ProductGrpcResponse>(
                service, METHODID_GET_PRODUCT_BY_ID)))
        .build();
  }

  private static abstract class ProductGrpcServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    ProductGrpcServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.ecommerce.grpc.ProductServiceProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("ProductGrpcService");
    }
  }

  private static final class ProductGrpcServiceFileDescriptorSupplier
      extends ProductGrpcServiceBaseDescriptorSupplier {
    ProductGrpcServiceFileDescriptorSupplier() {}
  }

  private static final class ProductGrpcServiceMethodDescriptorSupplier
      extends ProductGrpcServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    ProductGrpcServiceMethodDescriptorSupplier(java.lang.String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (ProductGrpcServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new ProductGrpcServiceFileDescriptorSupplier())
              .addMethod(getGetProductByIdMethod())
              .build();
        }
      }
    }
    return result;
  }
}
