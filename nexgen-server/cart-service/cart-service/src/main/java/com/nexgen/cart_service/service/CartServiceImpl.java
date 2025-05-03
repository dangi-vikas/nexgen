package com.nexgen.cart_service.service;

import com.nexgen.cart_service.dto.*;
import com.nexgen.cart_service.entity.CartItem;
import com.nexgen.cart_service.exception.CartEmptyException;
import com.nexgen.cart_service.exception.CartItemNotFoundException;
import com.nexgen.cart_service.exception.InvalidQuantityException;
import com.nexgen.cart_service.repository.CartItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CartServiceImpl implements CartService {

    private final CartItemRepository cartRepository;

    @Override
    public List<CartItemResponse> getCartByUser(String userId) {
        return cartRepository.findByUserId(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public CartItemResponse addItemToCart(String userId, CartItemRequest itemRequest) {
        CartItem item;

        if (cartRepository.existsByUserIdAndProductId(userId, itemRequest.getProductId())) {
            item = cartRepository.findByUserIdAndProductId(userId, itemRequest.getProductId());
            item.setQuantity(item.getQuantity() + itemRequest.getQuantity());
            item.setPrice(item.getPrice() + (itemRequest.getPrice() * itemRequest.getQuantity()));
        } else {
            item = CartItem.builder()
                    .userId(userId)
                    .productId(itemRequest.getProductId())
                    .quantity(itemRequest.getQuantity())
                    .price(itemRequest.getPrice() * itemRequest.getQuantity())
                    .build();
        }

        CartItem saved = cartRepository.save(item);
        return mapToResponse(saved);
    }

    @Override
    public CartItemResponse removeItemQuantity(String userId, String productId, int quantity) {
        if (quantity <= 0) {
            throw new InvalidQuantityException("Quantity to remove must be greater than 0.");
        }

        CartItem item = cartRepository.findByUserIdAndProductId(userId, productId);

        if (item == null) {
            throw new CartItemNotFoundException("Cart item not found for userId: " + userId + " and productId: " + productId);
        }

        if (item.getQuantity() < quantity) {
            throw new InvalidQuantityException("Reduce amount is greater than quantity in cart.");
        }

        int updatedQuantity = item.getQuantity() - quantity;
        double updatedPrice = item.getPrice() - (quantity * (item.getPrice() / item.getQuantity()));

        if (updatedQuantity > 0) {
            item.setQuantity(updatedQuantity);
            item.setPrice(updatedPrice);
            CartItem updated = cartRepository.save(item);
            return mapToResponse(updated);
        } else {
            cartRepository.delete(item);
            return CartItemResponse.builder()
                    .userId(userId)
                    .productId(productId)
                    .quantity(0)
                    .price(updatedPrice)
                    .build();
        }
    }

    @Override
    public void clearCart(String userId) {
        List<CartItem> items = cartRepository.findByUserId(userId);

        if (items.isEmpty()) {
            throw new CartEmptyException("Cart is already empty for userId: " + userId);
        }

        cartRepository.deleteByUserId(userId);
    }

    @Override
    public CheckoutResponse checkout(CheckoutRequest request) {
        List<CartItem> items = cartRepository.findByUserId(request.getUserId());

        double totalAmount = items.stream()
                .mapToDouble(CartItem::getPrice)
                .sum();

        List<String> purchasedProductIds = items.stream()
                .map(CartItem::getProductId)
                .collect(Collectors.toList());

        cartRepository.deleteAll(items);

        return CheckoutResponse.builder()
                .userId(request.getUserId())
                .paymentStatus("SUCCESS") // Simulated for now
                .totalAmount(totalAmount)
                .purchasedProductIds(purchasedProductIds)
                .build();
    }

    private CartItemResponse mapToResponse(CartItem item) {
        return CartItemResponse.builder()
                .userId(item.getUserId())
                .productId(item.getProductId())
                .quantity(item.getQuantity())
                .price(item.getPrice())
                .build();
    }
}
