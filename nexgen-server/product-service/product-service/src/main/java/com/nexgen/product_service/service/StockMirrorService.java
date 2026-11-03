package com.nexgen.product_service.service;

import com.nexgen.product_service.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Applies a stock level published by the inventory service to the catalogue row.
 *
 * <p>Kept out of the listener so the listener can record the event as processed only
 * after this transaction committed.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class StockMirrorService {

    private final ProductRepository productRepository;

    @Transactional
    public boolean syncQuantity(String skuCode, int quantity) {
        return productRepository.findBySkuCode(skuCode).map(product -> {
            product.setQuantity(quantity);
            productRepository.save(product);
            log.info("Mirrored stock level for {} to {}", skuCode, quantity);
            return true;
        }).orElseGet(() -> {
            log.debug("No product for SKU {}, ignoring stock event", skuCode);
            return false;
        });
    }
}
