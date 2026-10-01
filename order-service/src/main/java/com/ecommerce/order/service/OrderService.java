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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final InventoryRepository inventoryRepository;
    private final ProductGrpcClient productGrpcClient;

    /**
     * Tạo đơn hàng mới.
     * Luồng: gRPC lấy SP → Pessimistic Lock tồn kho → Snapshot → Lưu DB
     */
    @Transactional
    public OrderResponse createOrder(String userId, OrderRequest request) {
        List<OrderItem> items = new ArrayList<>();
        double totalPrice = 0.0;

        for (OrderItemRequest itemReq : request.getItems()) {
            // 1. Gọi gRPC sang product-service lấy thông tin để snapshot
            ProductGrpcResponse product = productGrpcClient.getProductById(itemReq.getProductId());

            // 2. Kiểm tra + Pessimistic Lock tồn kho (SELECT FOR UPDATE)
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
                .note(request.getNote())
                .build();

        // Gán order_id cho từng item
        Order savedOrder = orderRepository.save(order);
        items.forEach(item -> item.setOrder(savedOrder));
        savedOrder.getItems().addAll(items);
        orderRepository.save(savedOrder);

        log.info("Order #{} created for userId={}, total={}", savedOrder.getId(), userId, totalPrice);
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
                .note(order.getNote())
                .items(itemResponses)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
