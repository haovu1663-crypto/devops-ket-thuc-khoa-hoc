package com.ecommerce.product.controller;

import com.ecommerce.product.dto.ApiResponse;
import com.ecommerce.product.dto.ProductRequest;
import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * GET /api/v1/products - Xem danh sách sản phẩm (PUBLIC: Không cần Token).
     * Hỗ trợ tìm kiếm theo danh mục (?category=...) hoặc từ khóa (?keyword=...).
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getAllProducts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword) {

        List<ProductResponse> products = productService.getAllProducts(category, keyword);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách sản phẩm thành công", products));
    }

    /**
     * GET /api/v1/products/{id} - Xem chi tiết 1 sản phẩm (PUBLIC: Không cần Token).
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(@PathVariable Long id) {
        ProductResponse product = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết sản phẩm thành công", product));
    }

    /**
     * POST /api/v1/products - Thêm sản phẩm mới (CHỈ ROLE_ADMIN).
     * Header X-User-Role được inject bởi API Gateway sau khi xác thực Token.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @Valid @RequestBody ProductRequest request) {

        // Phân quyền theo vai trò (RBAC)
        if (!"ROLE_ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(403, "Từ chối truy cập: Chỉ Quản trị viên (ADMIN) mới có quyền thêm sản phẩm!"));
        }

        ProductResponse product = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Thêm sản phẩm mới thành công", product));
    }

    /**
     * PUT /api/v1/products/{id} - Cập nhật sản phẩm (CHỈ ROLE_ADMIN).
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @Valid @RequestBody ProductRequest request) {

        if (!"ROLE_ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(403, "Từ chối truy cập: Chỉ Quản trị viên (ADMIN) mới có quyền cập nhật sản phẩm!"));
        }

        ProductResponse product = productService.updateProduct(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật sản phẩm thành công", product));
    }

    /**
     * DELETE /api/v1/products/{id} - Xóa sản phẩm (CHỈ ROLE_ADMIN).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Role", required = false) String role) {

        if (!"ROLE_ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(403, "Từ chối truy cập: Chỉ Quản trị viên (ADMIN) mới có quyền xóa sản phẩm!"));
        }

        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa sản phẩm thành công", null));
    }
}
