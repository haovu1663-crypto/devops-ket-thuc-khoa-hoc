package com.ecommerce.product.service;

import com.ecommerce.product.dto.ProductRequest;
import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.model.Product;
import com.ecommerce.product.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     * Lấy danh sách sản phẩm, có hỗ trợ lọc theo category và keyword.
     */
    public List<ProductResponse> getAllProducts(String category, String keyword) {
        List<Product> products;

        if (category != null && !category.isBlank() && keyword != null && !keyword.isBlank()) {
            products = productRepository.findByCategoryIgnoreCaseAndNameContainingIgnoreCase(category, keyword);
        } else if (category != null && !category.isBlank()) {
            products = productRepository.findByCategoryIgnoreCase(category);
        } else if (keyword != null && !keyword.isBlank()) {
            products = productRepository.findByNameContainingIgnoreCase(keyword);
        } else {
            products = productRepository.findAll();
        }

        return products.stream()
                .map(ProductResponse::fromEntity)
                .toList();
    }

    /**
     * Lấy chi tiết một sản phẩm theo ID.
     */
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với id: " + id));
        return ProductResponse.fromEntity(product);
    }

    /**
     * Thêm sản phẩm mới.
     */
    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stock(request.getStock())
                .category(request.getCategory())
                .imageUrl(request.getImageUrl())
                .build();

        product = productRepository.save(product);
        log.info("Đã tạo sản phẩm mới thành công trong PostgreSQL: {} (ID: {})", product.getName(), product.getId());
        return ProductResponse.fromEntity(product);
    }

    /**
     * Cập nhật thông tin sản phẩm.
     */
    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với id: " + id));

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setCategory(request.getCategory());
        product.setImageUrl(request.getImageUrl());

        product = productRepository.save(product);
        log.info("Đã cập nhật sản phẩm thành công: {} (ID: {})", product.getName(), product.getId());
        return ProductResponse.fromEntity(product);
    }

    /**
     * Xóa sản phẩm.
     */
    @Transactional
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new RuntimeException("Không tìm thấy sản phẩm với id: " + id);
        }
        productRepository.deleteById(id);
        log.info("Đã xóa sản phẩm ID: {}", id);
    }
}
