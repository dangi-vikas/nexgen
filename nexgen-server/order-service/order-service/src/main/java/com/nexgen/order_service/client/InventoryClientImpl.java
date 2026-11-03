package com.nexgen.order_service.client;

import com.nexgen.order_service.dto.OrderItemRequest;
import com.nexgen.order_service.exception.InventoryUnavailableException;
import com.nexgen.order_service.exception.InvalidOrderException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class InventoryClientImpl implements InventoryClient {

    private final WebClient webClient;

    public InventoryClientImpl(WebClient.Builder builder, @Value("${app.services.inventory-url:http://localhost:8082}") String baseUrl) {
        this.webClient = builder.baseUrl(baseUrl).build();
    }

    @Override
    public void reserve(String orderNumber, List<OrderItemRequest> items, String authorizationHeader) {
        if (orderNumber == null || orderNumber.isBlank()) {
            throw new InvalidOrderException("orderNumber is required");
        }
        if (items == null || items.isEmpty()) {
            throw new InvalidOrderException("items are required");
        }
        List<Map<String, Object>> itemMaps = items.stream()
                .map(i -> {
                    Map<String, Object> m = new java.util.HashMap<>();
                    m.put("skuCode", i.getSkuCode());
                    m.put("quantity", i.getQuantity());
                    return m;
                })
                .collect(Collectors.toList());
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("orderNumber", orderNumber);
        body.put("items", itemMaps);
        WebClient.RequestBodySpec spec = webClient.post()
                .uri("/api/v1/inventory/reservations")
                .contentType(MediaType.APPLICATION_JSON);
        if (authorizationHeader != null && !authorizationHeader.isBlank()) {
            spec = spec.header(HttpHeaders.AUTHORIZATION, authorizationHeader);
        }
        try {
            spec.bodyValue(body)
                    .retrieve()
                    .onStatus(status -> status.value() == 409, clientResponse -> Mono.error(new InvalidOrderException("Insufficient stock")))
                    .onStatus(HttpStatusCode::is4xxClientError, clientResponse -> Mono.error(new InvalidOrderException("Inventory reservation failed")))
                    .onStatus(HttpStatusCode::is5xxServerError, clientResponse -> Mono.error(new InventoryUnavailableException("Inventory service unavailable")))
                    .bodyToMono(Map.class)
                    .block(Duration.ofSeconds(5));
        } catch (InvalidOrderException | InventoryUnavailableException e) {
            throw e;
        } catch (Exception e) {
            throw new InventoryUnavailableException("Inventory reservation failed");
        }
    }

    @Override
    public void release(String orderNumber, String authorizationHeader) {
        if (orderNumber == null || orderNumber.isBlank()) {
            return;
        }
        WebClient.RequestHeadersSpec<?> spec = webClient.delete()
                .uri("/api/v1/inventory/reservations/{orderNumber}", orderNumber);
        if (authorizationHeader != null && !authorizationHeader.isBlank()) {
            spec = spec.header(HttpHeaders.AUTHORIZATION, authorizationHeader);
        }
        try {
            spec.retrieve()
                    .onStatus(HttpStatusCode::is5xxServerError, clientResponse -> Mono.error(new InventoryUnavailableException("Inventory service unavailable")))
                    .bodyToMono(Void.class)
                    .block(Duration.ofSeconds(3));
        } catch (Exception ignored) {
            // best effort release
        }
    }
}
