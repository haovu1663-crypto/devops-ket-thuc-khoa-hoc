package com.ecommerce.order.service;

import com.ecommerce.grpc.ProductGrpcResponse;
import com.ecommerce.order.client.ProductGrpcClient;
import com.ecommerce.order.dto.InventoryRequest;
import com.ecommerce.order.dto.InventoryResponse;
import com.ecommerce.order.entity.Inventory;
import com.ecommerce.order.exception.BusinessException;
import com.ecommerce.order.exception.ResourceNotFoundException;
import com.ecommerce.order.repository.InventoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final InventoryRepository inventoryRepository;
    private final ProductGrpcClient productGrpcClient;

    /**
     * Admin thêm sản phẩm vào kho hoặc cộng dồn số lượng (Smart Upsert).
     * Luồng:
     * 1. Gọi gRPC sang product-service xác nhận SP tồn tại và lấy stock mặc định.
     * 2. Xác định số lượng nhập: ưu tiên request.quantity -> request.totalStock -> product.stock.
     * 3. Nếu SP chưa có trong kho: tạo mới bản ghi (totalStock = quantity, reservedStock = 0).
     * 4. Nếu SP đã có trong kho: cộng dồn vào totalStock hiện tại (totalStock += quantity).
     */
    @Transactional
    public InventoryResponse addOrUpdateInventory(InventoryRequest request) {
        // 1. Gọi gRPC sang product-service xác nhận sản phẩm tồn tại
        ProductGrpcResponse product = productGrpcClient.getProductById(request.getProductId());

        // 2. Xác định số lượng nhập
        int quantityToAdd;
        if (request.getQuantity() != null && request.getQuantity() > 0) {
            quantityToAdd = request.getQuantity();
        } else if (request.getTotalStock() != null && request.getTotalStock() > 0) {
            quantityToAdd = request.getTotalStock();
        } else {
            // Lấy trực tiếp từ stock của sản phẩm trên product-service (mặc định tối thiểu 1)
            quantityToAdd = Math.max(1, product.getStock());
        }

        // 3. Tìm xem sản phẩm đã có trong kho chưa
        return inventoryRepository.findByProductId(request.getProductId())
                .map(existing -> {
                    // ĐÃ CÓ: Cộng dồn thêm số lượng vào kho hiện tại
                    existing.setTotalStock(existing.getTotalStock() + quantityToAdd);
                    existing.setProductName(product.getName()); // Cập nhật tên nếu có thay đổi
                    Inventory updated = inventoryRepository.save(existing);
                    log.info("Cộng dồn kho cho sản phẩm '{}' (ID={}): thêm {}, tổng hiện tại={}",
                            product.getName(), product.getId(), quantityToAdd, updated.getTotalStock());
                    return mapToResponse(updated);
                })
                .orElseGet(() -> {
                    // CHƯA CÓ: Tạo mới bản ghi tồn kho
                    Inventory newInventory = Inventory.builder()
                            .productId(product.getId())
                            .productName(product.getName())
                            .totalStock(quantityToAdd)
                            .reservedStock(0)
                            .build();
                    Inventory saved = inventoryRepository.save(newInventory);
                    log.info("Khởi tạo kho mới cho sản phẩm '{}' (ID={}), số lượng ban đầu={}",
                            product.getName(), product.getId(), quantityToAdd);
                    return mapToResponse(saved);
                });
    }

    /** Giữ method alias cho tương thích ngược */
    @Transactional
    public InventoryResponse initInventory(InventoryRequest request) {
        return addOrUpdateInventory(request);
    }

    /**
     * Admin nhập thêm hàng vào kho (restock).
     * Luồng: Tìm inventory → gRPC xác nhận SP vẫn còn tồn tại → Cộng thêm
     */
    @Transactional
    public InventoryResponse restock(Long productId, int additionalStock) {
        if (additionalStock <= 0) {
            throw new BusinessException("Số lượng nhập thêm phải lớn hơn 0");
        }

        // 1. Tìm inventory theo productId
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Chưa có tồn kho cho sản phẩm ID=" + productId +
                        ". Dùng API khởi tạo tồn kho trước."));

        // 2. gRPC xác nhận sản phẩm vẫn tồn tại trong product-service
        ProductGrpcResponse product = productGrpcClient.getProductById(productId);
        log.info("Restock '{}' thêm {} đơn vị", product.getName(), additionalStock);

        // 3. Cộng thêm hàng
        inventory.setTotalStock(inventory.getTotalStock() + additionalStock);
        return mapToResponse(inventoryRepository.save(inventory));
    }

    /** Xem tồn kho của 1 sản phẩm (PUBLIC) */
    @Transactional(readOnly = true)
    public InventoryResponse getInventory(Long productId) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy tồn kho cho sản phẩm ID=" + productId));
        return mapToResponse(inventory);
    }

    private InventoryResponse mapToResponse(Inventory inventory) {
        return InventoryResponse.builder()
                .productId(inventory.getProductId())
                .productName(inventory.getProductName())
                .totalStock(inventory.getTotalStock())
                .reservedStock(inventory.getReservedStock())
                .availableStock(inventory.getAvailableStock())
                .updatedAt(inventory.getUpdatedAt())
                .build();
    }
}
