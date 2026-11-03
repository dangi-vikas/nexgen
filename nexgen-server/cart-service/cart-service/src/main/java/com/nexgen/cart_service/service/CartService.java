package com.nexgen.cart_service.service;

import com.nexgen.cart_service.dto.*;
import com.nexgen.cart_service.entity.CartItem;

import java.util.List;

public interface CartService {

    List<CartItemResponse> getCartByUser(String userId);
    CartItemResponse addItemToCart(String userId, CartItemRequest item);
    CartItemResponse removeItemQuantity(String userId, String productId, int quantity);
    void clearCart(String userId);
    CheckoutResponse checkout(CheckoutRequest request);
    CheckoutResponse checkout(CheckoutRequest request, String authorizationHeader);
    List<CartItem> getItemsByUserId(String userId);
}
