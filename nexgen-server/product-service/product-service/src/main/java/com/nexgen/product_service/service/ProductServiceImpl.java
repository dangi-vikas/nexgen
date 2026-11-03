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
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository repository;
    private final KafkaProducerService producer;
    private final RedisCacheUtil redisCacheUtil;
    private final ProductMetrics productMetrics;

    @CachePut(value = "products", key = "#product.skuCode")
    @Transactional
    @Override
    public Product createProduct(Product product) {
        if (repository.findBySkuCode(product.getSkuCode()).isPresent()) {
            throw new DuplicateProductException(product.getSkuCode());
        }

        Product savedProduct = repository.save(product);

        // Inventory creates the matching stock row from this event; the outbox row
        // commits with the product, so a created product is always announced.
        producer.sendProductCreatedEvent(ProductEvent.builder()
                .skuCode(savedProduct.getSkuCode())
                .eventType("CREATED")
                .name(savedProduct.getName())
                .quantity(savedProduct.getQuantity() == null ? 0 : savedProduct.getQuantity())
                .timestamp(System.currentTimeMillis())
                .build());

        productMetrics.incrementProductCreated();

        return savedProduct;
    }

    @CachePut(value = "products", key = "#skuCode")
    @Transactional
    @Override
    public Product updateProduct(String skuCode, Product product) {
        Product existing = repository.findBySkuCode(skuCode)
                .orElseThrow(() -> new ProductNotFoundException(skuCode));
        existing.setName(product.getName());
        existing.setDescription(product.getDescription());
        existing.setPrice(product.getPrice());
        existing.setQuantity(product.getQuantity());

        Product updated = repository.save(existing);

        producer.sendProductStockUpdatedEvent(ProductEvent.builder()
                .skuCode(updated.getSkuCode())
                .eventType("STOCK_UPDATED")
                .name(updated.getName())
                .quantity(updated.getQuantity() == null ? 0 : updated.getQuantity())
                .timestamp(System.currentTimeMillis())
                .build());

        productMetrics.incrementStockUpdated();

        return updated;
    }

    @CacheEvict(value = "products", key = "#skuCode")
    @Transactional
    @Override
    public void deleteProduct(String skuCode) {
        Product existing = repository.findBySkuCode(skuCode)
                .orElseThrow(() -> new ProductNotFoundException(skuCode));

        repository.delete(existing);

        producer.sendProductDeletedEvent(ProductEvent.builder()
                .skuCode(existing.getSkuCode())
                .eventType("DELETED")
                .name(existing.getName())
                .quantity(existing.getQuantity() == null ? 0 : existing.getQuantity())
                .timestamp(System.currentTimeMillis())
                .build());
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
