package com.nexgen.cart_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexgen.cart_service.dto.*;
import com.nexgen.cart_service.entity.CartItem;
import com.nexgen.cart_service.service.CartEventProducerService;
import com.nexgen.cart_service.service.CartService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest
@AutoConfigureMockMvc
public class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CartService cartService;

    @MockitoBean
    private CartEventProducerService cartEventProducer;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testGetCartByUser() throws Exception {
        String userId = "user1";
        CartItemResponse response = new CartItemResponse(userId, "product1", 2, 10.0);
        Mockito.when(cartService.getCartByUser(userId)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/cart/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productId").value("product1"))
                .andExpect(jsonPath("$[0].quantity").value(2));
    }

    @Test
    void testAddItemToCart() throws Exception {
        String userId = "user1";
        CartItemRequest request = new CartItemRequest("product1", 2, 5.0);
        CartItemResponse response = new CartItemResponse(userId, "product1", 2, 10.0);

        Mockito.when(cartService.addItemToCart(eq(userId), any(CartItemRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/cart/{userId}/add", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value("product1"))
                .andExpect(jsonPath("$.quantity").value(2));
    }

    @Test
    void testRemoveItemQuantity() throws Exception {
        String userId = "user1";
        String productId = "product1";
        int quantity = 1;

        CartItemResponse response = new CartItemResponse(userId, productId, 1, 10.0);
        Mockito.when(cartService.removeItemQuantity(userId, productId, quantity)).thenReturn(response);

        mockMvc.perform(delete("/api/v1/cart/{userId}/remove/{productId}/quantity/{quantity}", userId, productId, quantity))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(productId));
    }

    @Test
    void testClearCart() throws Exception {
        String userId = "user1";

        Mockito.doNothing().when(cartService).clearCart(userId);
        Mockito.when(cartService.getItemsByUserId(userId)).thenReturn(List.of(
                new CartItem(null, userId, "p1", 1, 10.0),
                new CartItem(null, userId, "p2", 2, 20.0)
        ));

        mockMvc.perform(delete("/api/v1/cart/{userId}/clear", userId))
                .andExpect(status().isNoContent());
    }

    @Test
    void testCheckout() throws Exception {
        CheckoutRequest request = new CheckoutRequest("user1", "CARD");
        CheckoutResponse response = new CheckoutResponse("user1", "success", 30.0, new ArrayList(Arrays.asList("p1", "p2")));

        Mockito.when(cartService.checkout(any(CheckoutRequest.class))).thenReturn(response);
        Mockito.when(cartService.getItemsByUserId("user1")).thenReturn(List.of(
                new CartItem(null, "user1", "p1", 1, 10.0),
                new CartItem(null, "user1", "p2", 2, 10.0)
        ));

        mockMvc.perform(post("/api/v1/cart/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("user1"))
                .andExpect(jsonPath("$.totalAmount").value(30.0));
    }
}
