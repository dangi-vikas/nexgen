package com.nexgen.product_service.exception;

public class DuplicateProductException extends RuntimeException {
    public DuplicateProductException(String skuCode) {
        super("Product with SKU Code '" + skuCode + "' already exists.");
    }
}
