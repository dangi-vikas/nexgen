package com.nexgen.product_service.exception;

public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(String skuCode) {
        super("Product not found with SKU: " + skuCode);
    }
}