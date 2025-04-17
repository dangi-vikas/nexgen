package com.nexgen.product_service.controller;

import com.nexgen.product_service.entity.Product;
import com.nexgen.product_service.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Product Controller", description = "Manage product catalog")
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "Create a new product")
    @PostMapping
    public ResponseEntity<Product> create(@Valid @RequestBody Product product) {
        return ResponseEntity.ok(productService.createProduct(product));
    }

    @Operation(summary = "Update an existing product by SKU code")
    @PutMapping("/{skuCode}")
    public ResponseEntity<Product> update(@PathVariable String skuCode,
                                          @Valid @RequestBody Product product) {
        return ResponseEntity.ok(productService.updateProduct(skuCode, product));
    }

    @Operation(summary = "Delete a product by SKU code")
    @DeleteMapping("/{skuCode}")
    public ResponseEntity<Void> delete(@PathVariable String skuCode) {
        productService.deleteProduct(skuCode);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get a product by SKU code")
    @GetMapping("/{skuCode}")
    public ResponseEntity<Product> getBySkuCode(@PathVariable String skuCode) {
        return ResponseEntity.ok(productService.getProductBySkuCode(skuCode));
    }

    @Operation(summary = "Get all products")
    @GetMapping
    public ResponseEntity<Page<Product>> getAll(Pageable pageable) {
        return ResponseEntity.ok(productService.getAllProducts(pageable));
    }
}