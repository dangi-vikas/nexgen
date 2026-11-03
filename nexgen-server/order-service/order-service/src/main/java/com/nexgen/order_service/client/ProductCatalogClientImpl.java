package com.nexgen.order_service.client;

import com.nexgen.order_service.exception.InvalidOrderException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class ProductCatalogClientImpl implements ProductCatalogClient {

    private final WebClient webClient;

    public ProductCatalogClientImpl(WebClient.Builder builder, @Value("${app.services.product-url:http://localhost:8083}") String baseUrl) {
        this.webClient = builder.baseUrl(baseUrl).build();
    }

    @Override
    public ProductSnapshot getProduct(String skuCode) {
        if (skuCode == null || skuCode.isBlank()) {
            throw new InvalidOrderException("SKU code is required");
        }
        try {
            Map<String, Object> response = webClient.get()
                    .uri("/api/v1/products/{skuCode}", skuCode)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, clientResponse -> Mono.error(new InvalidOrderException("Unknown product SKU: " + skuCode)))
                    .onStatus(HttpStatusCode::is5xxServerError, clientResponse -> Mono.error(new InvalidOrderException("Product service unavailable")))
                    .bodyToMono(Map.class)
                    .block(java.time.Duration.ofSeconds(3));
            if (response == null) {
                throw new InvalidOrderException("Unknown product SKU: " + skuCode);
            }
            Object priceObj = response.get("price");
            BigDecimal price = null;
            if (priceObj instanceof Number number) {
                price = BigDecimal.valueOf(number.doubleValue());
            } else if (priceObj != null) {
                try {
                    price = new BigDecimal(priceObj.toString());
                } catch (Exception e) {
                    price = null;
                }
            }
            if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
                throw new InvalidOrderException("Invalid product price for SKU: " + skuCode);
            }
            String name = response.get("name") == null ? skuCode : String.valueOf(response.get("name"));
            return new ProductSnapshot(String.valueOf(response.getOrDefault("skuCode", skuCode)), name, price);
        } catch (InvalidOrderException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidOrderException("Product lookup failed for SKU: " + skuCode);
        }
    }
}
