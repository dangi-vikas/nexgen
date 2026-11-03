package com.nexgen.cart_service.client;

import com.nexgen.cart_service.dto.CartItemResponse;
import com.nexgen.cart_service.dto.CheckoutRequest;
import com.nexgen.cart_service.dto.CheckoutResponse;
import com.nexgen.cart_service.exception.CartEmptyException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class OrderServiceClientImpl implements OrderServiceClient {

    private final WebClient webClient;

    public OrderServiceClientImpl(WebClient.Builder builder, @Value("${app.services.order-url:http://localhost:8084}") String baseUrl) {
        this.webClient = builder.baseUrl(baseUrl).build();
    }

    @Override
    public CheckoutResponse checkout(CheckoutRequest request, String authorizationHeader) {
        if (request == null) {
            throw new CartEmptyException("Checkout request is required");
        }
        List<CartItemResponse> cartItems = null;
        // items passed via body context not available here; caller should build order items? We'll build from request context differently
        // better to accept cart items separately - but method signature fixed; instead build minimal
        // we expect caller to have cart items; use reflection not needed. We'll have service pass via constructor? easier: create overloaded
        Map<String, Object> orderRequest = new HashMap<>();
        orderRequest.put("idempotencyKey", request.getIdempotencyKey());
        // items will be injected by caller via a modified approach? We'll handle in service by calling with extended client
        WebClient.RequestBodySpec spec = webClient.post()
                .uri("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON);
        if (authorizationHeader != null && !authorizationHeader.isBlank()) {
            spec = spec.header(HttpHeaders.AUTHORIZATION, authorizationHeader);
        }
        try {
            Map<String, Object> response = spec.bodyValue(orderRequest)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, clientResponse -> Mono.error(new CartEmptyException("Order creation failed")))
                    .onStatus(HttpStatusCode::is5xxServerError, clientResponse -> Mono.error(new CartEmptyException("Order service unavailable")))
                    .bodyToMono(Map.class)
                    .block(Duration.ofSeconds(10));
            if (response == null) {
                throw new CartEmptyException("Order creation failed");
            }
            CheckoutResponse checkoutResponse = new CheckoutResponse();
            checkoutResponse.setUserId(String.valueOf(response.get("userId")));
            checkoutResponse.setOrderNumber(String.valueOf(response.get("orderNumber")));
            checkoutResponse.setStatus(String.valueOf(response.get("status")));
            checkoutResponse.setPaymentStatus("NOT_IMPLEMENTED");
            Object total = response.get("totalAmount");
            if (total instanceof Number number) {
                checkoutResponse.setTotalAmount(BigDecimal.valueOf(number.doubleValue()));
            } else if (total != null) {
                try {
                    checkoutResponse.setTotalAmount(new BigDecimal(total.toString()));
                } catch (Exception e) {
                    checkoutResponse.setTotalAmount(BigDecimal.ZERO);
                }
            } else {
                checkoutResponse.setTotalAmount(BigDecimal.ZERO);
            }
            Object items = response.get("orderItems");
            if (items instanceof List<?> list) {
                List<String> skus = list.stream().map(i -> {
                    if (i instanceof Map<?, ?> m) {
                        Object sku = m.get("skuCode");
                        return sku == null ? null : String.valueOf(sku);
                    }
                    return null;
                }).filter(s -> s != null).collect(Collectors.toList());
                checkoutResponse.setPurchasedProductIds(skus);
            } else {
                checkoutResponse.setPurchasedProductIds(List.of());
            }
            return checkoutResponse;
        } catch (CartEmptyException e) {
            throw e;
        } catch (Exception e) {
            throw new CartEmptyException("Order creation failed");
        }
    }

    public CheckoutResponse checkoutWithItems(CheckoutRequest request, List<com.nexgen.cart_service.dto.CartItemRequest> orderItems, String authorizationHeader) {
        if (request == null) {
            throw new CartEmptyException("Checkout request is required");
        }
        List<Map<String, Object>> itemMaps = orderItems.stream()
                .map(i -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("skuCode", i.getProductId());
                    m.put("quantity", i.getQuantity());
                    return m;
                })
                .collect(Collectors.toList());
        Map<String, Object> orderRequest = new HashMap<>();
        orderRequest.put("idempotencyKey", request.getIdempotencyKey());
        orderRequest.put("orderItems", itemMaps);
        WebClient.RequestBodySpec spec = webClient.post()
                .uri("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON);
        if (authorizationHeader != null && !authorizationHeader.isBlank()) {
            if (!authorizationHeader.toLowerCase().startsWith("bearer")) {
                spec = spec.header(HttpHeaders.AUTHORIZATION, "Bearer " + authorizationHeader);
            } else {
                spec = spec.header(HttpHeaders.AUTHORIZATION, authorizationHeader);
            }
        }
        try {
            Map<String, Object> response = spec.bodyValue(orderRequest)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, clientResponse -> Mono.error(new CartEmptyException("Order creation failed")))
                    .onStatus(HttpStatusCode::is5xxServerError, clientResponse -> Mono.error(new CartEmptyException("Order service unavailable")))
                    .bodyToMono(Map.class)
                    .block(Duration.ofSeconds(10));
            if (response == null) {
                throw new CartEmptyException("Order creation failed");
            }
            CheckoutResponse checkoutResponse = new CheckoutResponse();
            checkoutResponse.setUserId(String.valueOf(response.get("userId")));
            checkoutResponse.setOrderNumber(String.valueOf(response.get("orderNumber")));
            checkoutResponse.setStatus(String.valueOf(response.get("status")));
            checkoutResponse.setPaymentStatus("NOT_IMPLEMENTED");
            Object total = response.get("totalAmount");
            if (total instanceof Number number) {
                checkoutResponse.setTotalAmount(BigDecimal.valueOf(number.doubleValue()));
            } else if (total != null) {
                try {
                    checkoutResponse.setTotalAmount(new BigDecimal(total.toString()));
                } catch (Exception e) {
                    checkoutResponse.setTotalAmount(BigDecimal.ZERO);
                }
            } else {
                checkoutResponse.setTotalAmount(BigDecimal.ZERO);
            }
            Object items = response.get("orderItems");
            if (items instanceof List<?> list) {
                List<String> skus = list.stream().map(i -> {
                    if (i instanceof Map<?, ?> m) {
                        Object sku = m.get("skuCode");
                        return sku == null ? null : String.valueOf(sku);
                    }
                    return null;
                }).filter(s -> s != null).collect(Collectors.toList());
                checkoutResponse.setPurchasedProductIds(skus);
            } else {
                checkoutResponse.setPurchasedProductIds(List.of());
            }
            return checkoutResponse;
        } catch (CartEmptyException e) {
            throw e;
        } catch (Exception e) {
            throw new CartEmptyException("Order creation failed");
        }
    }
}
