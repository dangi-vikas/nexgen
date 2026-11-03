package com.nexgen.cart_service.client;

import com.nexgen.cart_service.exception.InvalidQuantityException;
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
            throw new InvalidQuantityException("Product ID is required");
        }
        try {
            Map<String, Object> response = webClient.get()
                    .uri("/api/v1/products/{skuCode}", skuCode)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, clientResponse -> Mono.error(new InvalidQuantityException("Unknown product: " + skuCode)))
                    .onStatus(HttpStatusCode::is5xxServerError, clientResponse -> Mono.error(new InvalidQuantityException("Product service unavailable")))
                    .bodyToMono(Map.class)
                    .block(java.time.Duration.ofSeconds(3));
            if (response == null) {
                throw new InvalidQuantityException("Unknown product: " + skuCode);
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
                throw new InvalidQuantityException("Invalid product price");
            }
            String name = response.get("name") == null ? skuCode : String.valueOf(response.get("name"));
            return new ProductSnapshot(String.valueOf(response.getOrDefault("skuCode", skuCode)), name, price);
        } catch (InvalidQuantityException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidQuantityException("Product lookup failed");
        }
    }
}
