package com.nexgen.product_service.service;

import com.nexgen.product_service.entity.Product;
import com.nexgen.product_service.exception.DuplicateProductException;
import com.nexgen.product_service.exception.ProductNotFoundException;
import com.nexgen.product_service.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository repository;

    @Override
    public Product createProduct(Product product) {
        if (repository.findBySkuCode(product.getSkuCode()).isPresent()) {
            throw new DuplicateProductException(product.getSkuCode());
        }

        return repository.save(product);
    }

    @Override
    public Product updateProduct(String skuCode, Product product) {
        Product existing = repository.findBySkuCode(skuCode)
                .orElseThrow(() -> new ProductNotFoundException(skuCode));
        existing.setName(product.getName());
        existing.setDescription(product.getDescription());
        existing.setPrice(product.getPrice());
        existing.setQuantity(product.getQuantity());
        return repository.save(existing);
    }

    @Override
    public void deleteProduct(String skuCode) {
        Product existing = repository.findBySkuCode(skuCode)
                .orElseThrow(() -> new ProductNotFoundException(skuCode));
        repository.delete(existing);
    }

    @Override
    public Product getProductBySkuCode(String skuCode) {
        return repository.findBySkuCode(skuCode)
                .orElseThrow(() -> new ProductNotFoundException(skuCode));
    }

    @Override
    public Page<Product> getAllProducts(Pageable pageable) {
        return repository.findAll(pageable);
    }
}