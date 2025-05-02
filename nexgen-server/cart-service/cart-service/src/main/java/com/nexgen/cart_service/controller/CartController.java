package com.nexgen.cart_service.controller;

import com.nexgen.cart_service.dto.*;
import com.nexgen.cart_service.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cart")
@Tag(name = "Cart Controller", description = "Manage customer cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @Operation(summary = "Get current items in the cart")
    @GetMapping("/{userId}")
    public ResponseEntity<List<CartItemResponse>> getCartByUser(@PathVariable String userId) {
        return ResponseEntity.ok(cartService.getCartByUser(userId));
    }

    @Operation(summary = "Add an item to the cart")
    @PostMapping("/{userId}/add")
    public ResponseEntity<CartItemResponse> addItemToCart(
            @PathVariable String userId,
            @RequestBody CartItemRequest itemRequest
    ) {
        return ResponseEntity.ok(cartService.addItemToCart(userId, itemRequest));
    }

    @Operation(summary = "Remove items from the cart")
    @DeleteMapping("/{userId}/remove/{productId}/quantity/{quantity}")
    public ResponseEntity<CartItemResponse> removeItemQuantity(
            @PathVariable String userId,
            @PathVariable String productId,
            @PathVariable int quantity) {

        if (quantity <= 0) {
            return ResponseEntity.badRequest().body(null);
        }
        return ResponseEntity.ok(cartService.removeItemQuantity(userId, productId, quantity));
    }

    @Operation(summary = "Clear the cart")
    @DeleteMapping("/{userId}/clear")
    public ResponseEntity<Void> clearCart(@PathVariable String userId) {
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Pay and Checkout")
    @PostMapping("/checkout")
    public ResponseEntity<CheckoutResponse> checkout(@RequestBody CheckoutRequest request) {
        return ResponseEntity.ok(cartService.checkout(request));
    }
}
