package com.nexgen.cart_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexgen.cart_service.config.SecurityConfig;
import com.nexgen.cart_service.dto.CartItemRequest;
import com.nexgen.cart_service.dto.CartItemResponse;
import com.nexgen.cart_service.dto.CheckoutRequest;
import com.nexgen.cart_service.dto.CheckoutResponse;
import com.nexgen.cart_service.entity.CartItem;
import com.nexgen.cart_service.service.CartEventProducerService;
import com.nexgen.cart_service.service.CartService;
import com.nexgen.cart_service.support.JwtTestSupport;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CartController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "jwt.secret=01234567890123456789012345678901",
        "jwt.issuer=nexgen-user-service",
        "jwt.audience=nexgen-api"
})
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CartService cartService;

    @MockitoBean
    private CartEventProducerService cartEventProducer;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getCartReturnsOnlyAuthenticatedUsersCart() throws Exception {
        String userId = "user1";
        CartItemResponse response = new CartItemResponse(userId, "product1", 2, java.math.BigDecimal.valueOf(10.0), java.math.BigDecimal.valueOf(20.0));
        Mockito.when(cartService.getCartByUser(userId)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/cart/{userId}", userId)
                        .with(userJwt(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].productId").value("product1"))
                .andExpect(jsonPath("$.data[0].quantity").value(2));
    }

    @Test
    void getCartForAnotherUserIsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/cart/user1")
                        .with(userJwt("user2")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    void getCartWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/cart/user1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void addItemToCartUsesAuthenticatedUser() throws Exception {
        String userId = "user1";
        CartItemRequest request = new CartItemRequest("product1", 2);
        CartItemResponse response = new CartItemResponse(userId, "product1", 2, java.math.BigDecimal.valueOf(10.0), java.math.BigDecimal.valueOf(20.0));

        Mockito.when(cartService.addItemToCart(Mockito.eq(userId), Mockito.any(CartItemRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/cart/{userId}/add", userId)
                        .with(userJwt(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.productId").value("product1"))
                .andExpect(jsonPath("$.data.quantity").value(2));

        Mockito.verify(cartEventProducer).sendAddToCartEvent(Mockito.argThat(event ->
                event != null && userId.equals(event.getUserId()) && "product1".equals(event.getProductId())));
    }

    @Test
    void addItemToAnotherUsersCartIsForbidden() throws Exception {
        CartItemRequest request = new CartItemRequest("product1", 2);

        mockMvc.perform(post("/api/v1/cart/user1/add")
                        .with(userJwt("user2"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void removeItemQuantityUsesAuthenticatedUser() throws Exception {
        String userId = "user1";
        String productId = "product1";
        int quantity = 1;

        CartItemResponse response = new CartItemResponse(userId, productId, 1, java.math.BigDecimal.valueOf(10.0), java.math.BigDecimal.valueOf(10.0));
        Mockito.when(cartService.removeItemQuantity(userId, productId, quantity)).thenReturn(response);

        mockMvc.perform(delete("/api/v1/cart/{userId}/remove/{productId}/quantity/{quantity}", userId, productId, quantity)
                        .with(userJwt(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.productId").value(productId));
    }

    @Test
    void clearCartUsesAuthenticatedUser() throws Exception {
        String userId = "user1";

        Mockito.doNothing().when(cartService).clearCart(userId);
        Mockito.when(cartService.getItemsByUserId(userId)).thenReturn(List.of(
                CartItem.builder().userId(userId).productId("p1").quantity(1).price(java.math.BigDecimal.valueOf(10.0)).build(),
                CartItem.builder().userId(userId).productId("p2").quantity(2).price(java.math.BigDecimal.valueOf(20.0)).build()
        ));

        mockMvc.perform(delete("/api/v1/cart/{userId}/clear", userId)
                        .with(userJwt(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        Mockito.verify(cartEventProducer).sendCartClearedEvent(Mockito.argThat(event ->
                userId.equals(event.getUserId()) && event.getQuantity() == 3));
    }

    @Test
    void checkoutUsesAuthenticatedUser() throws Exception {
        String userId = "user1";
        CheckoutRequest request = new CheckoutRequest(userId, "CARD", "idem1");
        CheckoutResponse response = CheckoutResponse.builder()
                .userId(userId)
                .paymentStatus("NOT_IMPLEMENTED")
                .status("NOT_IMPLEMENTED")
                .totalAmount(java.math.BigDecimal.valueOf(30.0))
                .purchasedProductIds(List.of("p1", "p2"))
                .orderNumber("ORD1")
                .build();

        Mockito.when(cartService.checkout(Mockito.any(CheckoutRequest.class), Mockito.any())).thenReturn(response);
        Mockito.when(cartService.getItemsByUserId(userId)).thenReturn(List.of(
                CartItem.builder().userId(userId).productId("p1").quantity(1).price(java.math.BigDecimal.valueOf(10.0)).build(),
                CartItem.builder().userId(userId).productId("p2").quantity(2).price(java.math.BigDecimal.valueOf(10.0)).build()
        ));

        mockMvc.perform(post("/api/v1/cart/checkout")
                        .with(userJwt(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(userId))
                .andExpect(jsonPath("$.data.totalAmount").value(30.0));

        Mockito.verify(cartEventProducer).sendCheckoutEvent(Mockito.argThat(event ->
                userId.equals(event.getUserId()) && event.getTotalAmount() == 30.0));
    }

    @Test
    void checkoutForAnotherUserIsForbidden() throws Exception {
        CheckoutRequest request = new CheckoutRequest("user1", "CARD", "idem1");

        mockMvc.perform(post("/api/v1/cart/checkout")
                        .with(userJwt("user2"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void realUserTokenCanReadOwnCart() throws Exception {
        String userId = "user1";
        CartItemResponse response = new CartItemResponse(userId, "product1", 2, java.math.BigDecimal.valueOf(10.0), java.math.BigDecimal.valueOf(20.0));
        Mockito.when(cartService.getCartByUser(userId)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/cart/{userId}", userId)
                        .header("Authorization", "Bearer " + JwtTestSupport.accessToken(userId, "ROLE_USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].productId").value("product1"));
    }

    @Test
    void realUserTokenCannotReadAnotherUsersCart() throws Exception {
        mockMvc.perform(get("/api/v1/cart/user1")
                        .header("Authorization", "Bearer " + JwtTestSupport.accessToken("user2", "ROLE_USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void refreshTokenIsRejectedOnCartAccess() throws Exception {
        mockMvc.perform(get("/api/v1/cart/user1")
                        .header("Authorization", "Bearer " + JwtTestSupport.refreshToken("user1")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenWithWrongAudienceIsRejectedOnCartAccess() throws Exception {
        String token = JwtTestSupport.token("nexgen-user-service", "some-other-api",
                "access", "user1", "ROLE_USER");

        mockMvc.perform(get("/api/v1/cart/user1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor userJwt(String subject) {
        return jwt().jwt(jwt -> jwt.subject(subject))
                .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }
}
