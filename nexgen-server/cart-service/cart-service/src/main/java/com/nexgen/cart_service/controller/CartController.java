package com.nexgen.cart_service.controller;

import com.nexgen.cart_service.dto.*;
import com.nexgen.cart_service.entity.CartItem;
import com.nexgen.cart_service.exception.InvalidQuantityException;
import com.nexgen.cart_service.service.CartEventProducerService;
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
    private final CartEventProducerService cartEventProducer;

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

        double priceD = 0.0;
        if (item.getPrice() != null) {
            priceD = item.getPrice().doubleValue();
        }
        cartEventProducer.sendAddToCartEvent(
                new AddToCartEvent(userId, item.getProductId(), item.getQuantity(), priceD)
        );

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

        cartEventProducer.sendRemoveFromCartEvent(
                new RemoveFromCartEvent(userId, productId, quantity, "Item removed from cart")
        );

        return ResponseEntity.ok(ApiResponse.success(item, "Item quantity removed from cart"));
    }

    @Operation(summary = "Clear the cart")
    @DeleteMapping("/{userId}/clear")
    public ResponseEntity<ApiResponse<Void>> clearCart(
            @PathVariable String userId, Authentication authentication) {
        requireOwner(userId, authentication);
        cartService.clearCart(userId);

        List<CartItem> itemsCleared = cartService.getItemsByUserId(userId);
        int totalQuantityCleared = itemsCleared == null ? 0 : itemsCleared.stream().mapToInt(CartItem::getQuantity).sum();

        CartClearedEvent event = new CartClearedEvent(userId, totalQuantityCleared, "Cart cleared successfully.");
        cartEventProducer.sendCartClearedEvent(event);

        return ResponseEntity.ok(ApiResponse.success(null, "Cart cleared successfully"));
    }


    @Operation(summary = "Pay and Checkout")
    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<CheckoutResponse>> checkout(
            @Valid @RequestBody CheckoutRequest request, Authentication authentication,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        requireOwner(request.getUserId(), authentication);
        CheckoutResponse response = cartService.checkout(request, authorizationHeader);
        List<CartItem> items = cartService.getItemsByUserId(request.getUserId());

        cartEventProducer.sendCheckoutEvent(
                new CheckoutEvent(
                        request.getUserId(),
                        items.stream().map(CartItem::getProductId).collect(java.util.stream.Collectors.toList()),
                        response.getTotalAmount() == null ? 0.0 : response.getTotalAmount().doubleValue(),
                        "Checkout completed"
                )
        );

        return ResponseEntity.ok(ApiResponse.success(response, "Checkout processed"));
    }

    private void requireOwner(String userId, Authentication authentication) {
        if (authentication == null || !userId.equals(authentication.getName())) {
            throw new AccessDeniedException("You may only access your own cart");
        }
    }
}
