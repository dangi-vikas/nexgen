package com.nexgen.product_service.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class ProductMetrics {

    private final MeterRegistry meterRegistry;

    public ProductMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void incrementProductCreated() {
        meterRegistry.counter("product.created.count").increment();
    }

    public void incrementStockUpdated() {
        meterRegistry.counter("product.stock.updated.count").increment();
    }
}

