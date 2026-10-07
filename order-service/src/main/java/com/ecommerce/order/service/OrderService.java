package com.ecommerce.order.service;

import com.ecommerce.grpc.ProductGrpcResponse;
import com.ecommerce.order.client.ProductGrpcClient;
import com.ecommerce.order.dto.*;
import com.ecommerce.order.entity.Inventory;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderItem;
import com.ecommerce.order.entity.OrderStatus;
import com.ecommerce.order.exception.*;
import com.ecommerce.order.repository.InventoryRepository;
import com.ecommerce.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final InventoryRepository inventoryRepository;
    private final ProductGrpcClient productGrpcClient;
    private final RedissonClient redissonClient;
    private final TransactionTemplate transactionTemplate;
    private final com.ecommerce.order.event.OrderEventPublisher orderEventPublisher;

    /**
     * Tạo đơn hàng mới với Redis Distributed Lock.
     * Luồng:
     * 1. Thu thập và sắp xếp các Product ID tăng dần (chống Deadlock).
     * 2. Lấy Redisson Lock cho từng sản phẩm (waitTime: 3s, leaseTime: 5s).
     * 3. Thực thi trừ tồn kho & tạo Order trong DB Transaction (TransactionTemplate).
     * 4. Giải phóng toàn bộ Lock an toàn trong khối finally sau khi Transaction đã commit.
     * 5. Phát sự kiện OrderCreatedEvent sang Payment Service và các listener khác.
     */
    public OrderResponse createOrder(String userId, OrderRequest request) {
        // 1. Sắp xếp Product ID tăng dần để tránh Deadlock giữa các giao dịch đồng thời
        List<Long> productIds = request.getItems().stream()
                .map(OrderItemRequest::getProductId)
                .distinct()
                .sorted()
                .toList();

        List<RLock> acquiredLocks = new ArrayList<>();

        try {
            // 2. Thâu tóm Distributed Lock cho từng sản phẩm
            for (Long productId : productIds) {
                String lockKey = "lock:inventory:" + productId;
                RLock lock = redissonClient.getLock(lockKey);

                // Chờ tối đa 3s để lấy lock, tự động nhả sau 5s nếu sự cố
                boolean acquired = lock.tryLock(3, 5, TimeUnit.SECONDS);
                if (!acquired) {
                    log.warn("Failed to acquire Redis lock for productId={}, userId={}", productId, userId);
                    throw new BusinessException("Hệ thống đang xử lý lượng đặt hàng lớn cho sản phẩm này. Vui lòng thử lại sau!");
                }
                acquiredLocks.add(lock);
            }

            // 3. Thực thi nghiệp vụ kiểm tra tồn kho & Đặt hàng trong Transaction
            OrderResponse response = transactionTemplate.execute(status -> doCreateOrderWithStockDeduction(userId, request));

            // 4. Phát sự kiện OrderCreatedEvent cho Payment Service & Notification Service
            if (response != null) {
                orderEventPublisher.publishOrderCreated(com.ecommerce.order.event.OrderCreatedEvent.builder()
                        .orderId(response.getId())
                        .userId(userId)
                        .totalPrice(response.getTotalPrice())
                        .paymentMethod(request.getPaymentMethod())
                        .bankCode(request.getBankCode())
                        .createdAt(response.getCreatedAt())
                        .build());
            }

            return response;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("Giao dịch đặt hàng bị gián đoạn. Vui lòng thử lại!");
        } finally {
            // 5. Luôn giải phóng tất cả Lock theo thứ tự ngược lại
            for (int i = acquiredLocks.size() - 1; i >= 0; i--) {
                RLock lock = acquiredLocks.get(i);
                try {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                } catch (Exception e) {
                    log.error("Error releasing Redis lock: {}", e.getMessage(), e);
                }
            }
        }
    }

    /**
     * Nghiệp vụ trừ kho và tạo đơn hàng chạy trong Transaction
     */
    private OrderResponse doCreateOrderWithStockDeduction(String userId, OrderRequest request) {
        List<OrderItem> items = new ArrayList<>();
        double totalPrice = 0.0;

        for (OrderItemRequest itemReq : request.getItems()) {
            // 1. Gọi gRPC sang product-service lấy thông tin để snapshot
            ProductGrpcResponse product = productGrpcClient.getProductById(itemReq.getProductId());

           
            // 2. Kiểm tra + Pessimistic Lock tồn kho (SELECT FOR UPDATE)(SELECT * FROM inventories WHERE product_id = ? FOR UPDATE;)
            // đoạn lệnh này khóa database để sử lý không cho các hoạt động khác sửa đổi cùng lúc 
            Inventory inventory = inventoryRepository
                    .findByProductIdWithLock(itemReq.getProductId())
                    .orElseThrow(() -> new BusinessException(
                            "Sản phẩm '" + product.getName() + "' chưa được khởi tạo tồn kho. " +
                            "Vui lòng liên hệ Admin."));

            // 3. Kiểm tra đủ hàng không
            if (inventory.getAvailableStock() < itemReq.getQuantity()) {
                throw new InsufficientStockException(
                        "Sản phẩm '" + product.getName() + "' không đủ hàng. " +
                        "Yêu cầu: " + itemReq.getQuantity() +
                        " | Còn lại: " + inventory.getAvailableStock());
            }

            // 4. Giữ hàng (reserve stock)
            //Tạo thời trừ tạm số lượng để người khác vào  xem với sô lượng đã trừ
            inventory.setReservedStock(inventory.getReservedStock() + itemReq.getQuantity());
            inventoryRepository.save(inventory);
            log.info("Reserved {} units of product '{}' (productId={})",
                    itemReq.getQuantity(), product.getName(), itemReq.getProductId());

            // 5. Tạo order item với SNAPSHOT thông tin từ gRPC
            double subtotal = product.getPrice() * itemReq.getQuantity();
            OrderItem item = OrderItem.builder()
                    .productId(product.getId())
                    .productName(product.getName())    // ← Snapshot tên
                    .unitPrice(product.getPrice())     // ← Snapshot giá
                    .quantity(itemReq.getQuantity())
                    .subtotal(subtotal)
                    .build();

            items.add(item);
            totalPrice += subtotal;
        }

        // 6. Lưu order vào DB
        Order order = Order.builder()
                .userId(userId)
                .status(OrderStatus.PENDING)
                .totalPrice(totalPrice)
                .paymentMethod(request.getPaymentMethod())
                .note(request.getNote())
                .build();

        // Gán order_id cho từng item
        Order savedOrder = orderRepository.save(order);
        items.forEach(item -> item.setOrder(savedOrder));
        savedOrder.getItems().addAll(items);
        orderRepository.save(savedOrder);

        log.info("Order #{} created for userId={}, total={}, paymentMethod={}", savedOrder.getId(), userId, totalPrice, request.getPaymentMethod());
        return mapToResponse(savedOrder);
    }

    /** Lịch sử đơn hàng của user hiện tại */
    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders(String userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    /** Chi tiết 1 đơn hàng — user chỉ xem được đơn của mình */
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId, String userId, String role) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng #" + orderId));

        // User chỉ xem được đơn của mình; Admin xem được tất cả
        if (!"ROLE_ADMIN".equals(role) && !order.getUserId().equals(userId)) {
            throw new AccessDeniedException("Bạn không có quyền xem đơn hàng này");
        }
        return mapToResponse(order);
    }

    /**
     * Hủy đơn hàng — chỉ được khi PENDING.
     * Trả lại reserved_stock khi hủy.
     */
    @Transactional
    public OrderResponse cancelOrder(Long orderId, String userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng #" + orderId));

        if (!order.getUserId().equals(userId)) {
            throw new AccessDeniedException("Bạn không có quyền hủy đơn hàng này");
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessException("Chỉ có thể hủy đơn hàng ở trạng thái PENDING. " +
                    "Trạng thái hiện tại: " + order.getStatus());
        }

        // Trả lại hàng đã giữ vào kho
        releaseReservedStock(order);
        order.setStatus(OrderStatus.CANCELLED);
        log.info("Order #{} cancelled by userId={}", orderId, userId);
        return mapToResponse(orderRepository.save(order));
    }

    /**
     * Admin cập nhật trạng thái đơn hàng.
     * Khi CONFIRMED → COMPLETED: trừ total_stock (hàng thực sự xuất kho).
     * Khi → CANCELLED: trả lại reserved_stock.
     */
    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, UpdateStatusRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng #" + orderId));

        OrderStatus newStatus = request.getStatus();
        OrderStatus currentStatus = order.getStatus();

        // Validate chuyển trạng thái hợp lệ
        validateStatusTransition(currentStatus, newStatus);

        // Khi hoàn thành: trừ total_stock thực tế và giảm reserved_stock
        if (newStatus == OrderStatus.COMPLETED) {
            for (OrderItem item : order.getItems()) {
                inventoryRepository.findByProductIdWithLock(item.getProductId())
                        .ifPresent(inv -> {
                            inv.setTotalStock(inv.getTotalStock() - item.getQuantity());
                            inv.setReservedStock(inv.getReservedStock() - item.getQuantity());
                            inventoryRepository.save(inv);
                        });
            }
        }

        // Khi hủy bởi admin: trả lại reserved_stock
        if (newStatus == OrderStatus.CANCELLED && currentStatus == OrderStatus.PENDING) {
            releaseReservedStock(order);
        }

        order.setStatus(newStatus);
        log.info("Order #{} status changed: {} → {}", orderId, currentStatus, newStatus);
        return mapToResponse(orderRepository.save(order));
    }

    /** Admin xem tất cả đơn hàng */
    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll()
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    // ========== Private helpers ==========

    private void releaseReservedStock(Order order) {
        for (OrderItem item : order.getItems()) {
            inventoryRepository.findByProductId(item.getProductId())
                    .ifPresent(inv -> {
                        inv.setReservedStock(
                                Math.max(0, inv.getReservedStock() - item.getQuantity()));
                        inventoryRepository.save(inv);
                    });
        }
    }

    private void validateStatusTransition(OrderStatus current, OrderStatus next) {
        boolean valid = switch (current) {
            case PENDING    -> next == OrderStatus.CONFIRMED || next == OrderStatus.CANCELLED;
            case CONFIRMED  -> next == OrderStatus.SHIPPING  || next == OrderStatus.CANCELLED;
            case SHIPPING   -> next == OrderStatus.COMPLETED;
            default -> false;
        };
        if (!valid) {
            throw new BusinessException("Không thể chuyển trạng thái từ " + current + " sang " + next);
        }
    }

    private OrderResponse mapToResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> OrderItemResponse.builder()
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .unitPrice(item.getUnitPrice())
                        .quantity(item.getQuantity())
                        .subtotal(item.getSubtotal())
                        .build())
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .status(order.getStatus())
                .totalPrice(order.getTotalPrice())
                .paymentMethod(order.getPaymentMethod())
                .note(order.getNote())
                .items(itemResponses)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
