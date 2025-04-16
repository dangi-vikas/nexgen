package com.nexgen.product_service.service;

import com.nexgen.product_service.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductService {
    Product createProduct(Product product);
    Product updateProduct(String skuCode, Product product);
    void deleteProduct(String skuCode);
    Product getProductBySkuCode(String skuCode);
    Page<Product> getAllProducts(Pageable pageable);
}