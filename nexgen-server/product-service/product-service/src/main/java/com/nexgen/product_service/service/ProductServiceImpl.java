package com.nexgen.product_service.service;

import com.nexgen.product_service.config.PageCacheUtil;
import com.nexgen.product_service.config.RedisCacheUtil;
import com.nexgen.product_service.dto.ProductEvent;
import com.nexgen.product_service.dto.RedisPageWrapper;
import com.nexgen.product_service.entity.Product;
import com.nexgen.product_service.exception.DuplicateProductException;
import com.nexgen.product_service.exception.ProductNotFoundException;
import com.nexgen.product_service.metrics.ProductMetrics;
import com.nexgen.product_service.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository repository;
    private final KafkaProducerService producer;
    private final RedisCacheUtil redisCacheUtil;
    private final ProductMetrics productMetrics;

    @CachePut(value = "products", key = "#product.skuCode")
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

        productMetrics.incrementProductCreated();

        return savedProduct;
    }

    @CachePut(value = "products", key = "#skuCode")
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

        productMetrics.incrementStockUpdated();

        return updated;
    }

    @CacheEvict(value = "products", key = "#skuCode")
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

    @Cacheable(value = "products", key = "#skuCode")
    @Override
    public Product getProductBySkuCode(String skuCode) {
        return repository.findBySkuCode(skuCode)
                .orElseThrow(() -> new ProductNotFoundException(skuCode));
    }

    @Cacheable(value = "allProducts", key = "'page:' + #pageable.pageNumber + ':size:' + #pageable.pageSize")
    @Override
    public RedisPageWrapper<Product> getAllProducts(Pageable pageable) {
        Page<Product> page = repository.findAll(pageable);
        return PageCacheUtil.wrap(page);
    }
}