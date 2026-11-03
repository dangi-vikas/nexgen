package com.nexgen.order_service.client;

import java.math.BigDecimal;

public interface ProductCatalogClient {
    ProductSnapshot getProduct(String skuCode);

    record ProductSnapshot(String skuCode, String name, BigDecimal price) {}
}
