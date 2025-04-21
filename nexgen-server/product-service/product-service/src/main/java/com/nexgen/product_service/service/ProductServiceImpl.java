package com.nexgen.product_service.service;

import com.nexgen.product_service.dto.ProductEvent;
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
    private final KafkaProducerService producer;

    @Override
    public Product createProduct(Product product) {
        if (repository.findBySkuCode(product.getSkuCode()).isPresent()) {
            throw new DuplicateProductException(product.getSkuCode());
        }

        Product savedProduct = repository.save(product);

        producer.sendProductCreatedEvent(
                new ProductEvent(
                        savedProduct.getSkuCode(),
                        "CREATED",
                        savedProduct.getName(),
                        savedProduct.getQuantity(),
                        System.currentTimeMillis()
                )
        );

        return savedProduct;
    }

    @Override
    public Product updateProduct(String skuCode, Product product) {
        Product existing = repository.findBySkuCode(skuCode)
                .orElseThrow(() -> new ProductNotFoundException(skuCode));
        existing.setName(product.getName());
        existing.setDescription(product.getDescription());
        existing.setPrice(product.getPrice());
        existing.setQuantity(product.getQuantity());

        Product updated = repository.save(existing);

        producer.sendProductStockUpdatedEvent(new ProductEvent(
                updated.getSkuCode(),
                "STOCK_UPDATED",
                updated.getName(),
                updated.getQuantity(),
                System.currentTimeMillis()
        ));

        return updated;
    }

    @Override
    public void deleteProduct(String skuCode) {
        Product existing = repository.findBySkuCode(skuCode)
                .orElseThrow(() -> new ProductNotFoundException(skuCode));

        repository.delete(existing);

        producer.sendProductDeletedEvent(new ProductEvent(
                existing.getSkuCode(),
                "DELETED",
                existing.getName(),
                existing.getQuantity(),
                System.currentTimeMillis()
        ));
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