package com.nexgen.cart_service.controller;

import com.nexgen.cart_service.dto.*;
import com.nexgen.cart_service.exception.InvalidQuantityException;
import com.nexgen.cart_service.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
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
    public ResponseEntity<ApiResponse<List<CartItemResponse>>> getCartByUser(
            @PathVariable String userId, Authentication authentication) {
        requireOwner(userId, authentication);
        List<CartItemResponse> items = cartService.getCartByUser(userId);
        return ResponseEntity.ok(ApiResponse.success(items, "Cart items retrieved successfully"));
    }

    @Operation(summary = "Add an item to the cart")
    @PostMapping("/{userId}/add")
    public ResponseEntity<ApiResponse<CartItemResponse>> addItemToCart(
            @PathVariable String userId,
            @Valid @RequestBody CartItemRequest itemRequest,
            Authentication authentication
    ) {
        requireOwner(userId, authentication);
        CartItemResponse item = cartService.addItemToCart(userId, itemRequest);
        return ResponseEntity.ok(ApiResponse.success(item, "Item added to cart"));
    }

    @Operation(summary = "Remove items from the cart")
    @DeleteMapping("/{userId}/remove/{productId}/quantity/{quantity}")
    public ResponseEntity<ApiResponse<CartItemResponse>> removeItemQuantity(
            @PathVariable String userId,
            @PathVariable String productId,
            @PathVariable int quantity,
            Authentication authentication) {

        requireOwner(userId, authentication);

        if (quantity <= 0) {
            throw new InvalidQuantityException("Quantity to remove must be greater than 0.");
        }

        CartItemResponse item = cartService.removeItemQuantity(userId, productId, quantity);
        return ResponseEntity.ok(ApiResponse.success(item, "Item quantity removed from cart"));
    }

    @Operation(summary = "Clear the cart")
    @DeleteMapping("/{userId}/clear")
    public ResponseEntity<ApiResponse<Void>> clearCart(
            @PathVariable String userId, Authentication authentication) {
        requireOwner(userId, authentication);
        cartService.clearCart(userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Cart cleared successfully"));
    }


    @Operation(summary = "Pay and Checkout")
    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<CheckoutResponse>> checkout(
            @Valid @RequestBody CheckoutRequest request, Authentication authentication,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        requireOwner(request.getUserId(), authentication);
        // The cart is kept until order-confirmed arrives: checkout only means the
        // order was accepted and is waiting for its stock reservation.
        CheckoutResponse response = cartService.checkout(request, authorizationHeader);
        return ResponseEntity.ok(ApiResponse.success(response, "Checkout accepted"));
    }

    private void requireOwner(String userId, Authentication authentication) {
        if (authentication == null || !userId.equals(authentication.getName())) {
            throw new AccessDeniedException("You may only access your own cart");
        }
    }
}
