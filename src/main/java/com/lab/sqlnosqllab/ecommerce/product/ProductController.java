package com.lab.sqlnosqllab.ecommerce.product;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;
    private final ProductRepository productRepository;

    public ProductController(ProductService productService, ProductRepository productRepository) {
        this.productService = productService;
        this.productRepository = productRepository;
    }

    public record CreateProductRequest(
            @NotBlank String sku,
            @NotBlank String name,
            @NotBlank String category,
            @NotNull @Positive BigDecimal price,
            @NotNull @Min(0) Integer stock,
            Map<String, Object> attributes
    ) {
    }

    public record UpdateStockRequest(@Min(1) int quantityToAdd) {
    }

    @PostMapping
    public ResponseEntity<Product> create(@Valid @RequestBody CreateProductRequest request) {
        Product product = new Product();
        product.setSku(request.sku());
        product.setName(request.name());
        product.setCategory(request.category());
        product.setPrice(request.price());
        product.setStock(request.stock());
        product.setAttributes(request.attributes());

        Product saved = productService.create(product);
        return ResponseEntity
                .created(URI.create("/api/v1/products/" + saved.getId()))
                .body(saved);
    }

    @GetMapping
    public ResponseEntity<Page<Product>> list(
            @RequestParam(required = false) String category,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(productService.findAll(category, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getById(@PathVariable String id) {
        // Aqui retorna o documento completo, incluindo as reviews embutidas
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Produto não encontrado: " + id));
        return ResponseEntity.ok(product);
    }

    @PatchMapping("/{id}/stock")
    public ResponseEntity<Product> addStock(
            @PathVariable String id,
            @Valid @RequestBody UpdateStockRequest request) {
        return ResponseEntity.ok(productService.addStock(id, request.quantityToAdd()));
    }
}
