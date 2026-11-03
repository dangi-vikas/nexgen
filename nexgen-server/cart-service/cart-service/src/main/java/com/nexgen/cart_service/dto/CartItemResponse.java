package com.nexgen.cart_service.dto;

import java.math.BigDecimal;

public class CartItemResponse {
    private String userId;
    private String productId;
    private int quantity;
    private BigDecimal price;
    private BigDecimal lineTotal;

    public CartItemResponse() {
    }

    public CartItemResponse(String userId, String productId, int quantity, BigDecimal price, BigDecimal lineTotal) {
        this.userId = userId;
        this.productId = productId;
        this.quantity = quantity;
        this.price = price;
        this.lineTotal = lineTotal;
    }

    public static CartItemResponseBuilder builder() {
        return new CartItemResponseBuilder();
    }

    public static class CartItemResponseBuilder {
        private String userId;
        private String productId;
        private int quantity;
        private BigDecimal price;
        private BigDecimal lineTotal;

        public CartItemResponseBuilder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public CartItemResponseBuilder productId(String productId) {
            this.productId = productId;
            return this;
        }

        public CartItemResponseBuilder quantity(int quantity) {
            this.quantity = quantity;
            return this;
        }

        public CartItemResponseBuilder price(BigDecimal price) {
            this.price = price;
            return this;
        }

        public CartItemResponseBuilder lineTotal(BigDecimal lineTotal) {
            this.lineTotal = lineTotal;
            return this;
        }

        public CartItemResponse build() {
            return new CartItemResponse(userId, productId, quantity, price, lineTotal);
        }
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }

    public void setLineTotal(BigDecimal lineTotal) {
        this.lineTotal = lineTotal;
    }
}
