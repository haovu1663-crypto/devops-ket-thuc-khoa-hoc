package com.ecommerce.order.controller;

import com.ecommerce.order.dto.ApiResponse;
import com.ecommerce.order.dto.InventoryRequest;
import com.ecommerce.order.dto.InventoryResponse;
import com.ecommerce.order.service.InventoryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    /**
     * POST /api/v1/inventory — Admin thêm sản phẩm vào kho hoặc cộng dồn số lượng.
     * Tự động lấy stock từ product-service qua gRPC nếu không truyền số lượng.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<InventoryResponse>> addOrUpdateInventory(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @Valid @RequestBody InventoryRequest request) {

        if (!"ROLE_ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(403, "Chỉ Admin mới có quyền thao tác tồn kho"));
        }

        InventoryResponse response = inventoryService.addOrUpdateInventory(request);
        return ResponseEntity.ok(ApiResponse.success("Thao tác thêm/cộng dồn tồn kho thành công", response));
    }

    /**
     * GET /api/v1/inventory/{productId} — Xem tồn kho (PUBLIC, không cần token).
     */
    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<InventoryResponse>> getInventory(
            @PathVariable Long productId) {

        InventoryResponse response = inventoryService.getInventory(productId);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin tồn kho thành công", response));
    }

    /**
     * PATCH /api/v1/inventory/{productId}/restock — Admin nhập thêm hàng.
     * Gọi gRPC xác nhận SP vẫn còn tồn tại trước khi nhập hàng.
     */
    @PatchMapping("/{productId}/restock")
    public ResponseEntity<ApiResponse<InventoryResponse>> restock(
            @PathVariable Long productId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestParam @Min(value = 1, message = "Số lượng nhập thêm phải >= 1") int additionalStock) {

        if (!"ROLE_ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(403, "Chỉ Admin mới có quyền nhập thêm hàng"));
        }

        InventoryResponse response = inventoryService.restock(productId, additionalStock);
        return ResponseEntity.ok(ApiResponse.success(
                "Nhập thêm " + additionalStock + " đơn vị thành công", response));
    }
}
