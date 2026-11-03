package com.nexgen.cart_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "cart_items", uniqueConstraints = {
        @UniqueConstraint(name = "uk_cart_user_product", columnNames = {"userId", "productId"})
})
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String productId;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    @Version
    @Builder.Default
    private Long version = 0L;

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public CartItem() {
    }

    public CartItem(Long id, String userId, String productId, int quantity, BigDecimal price, Long version) {
        this.id = id;
        this.userId = userId;
        this.productId = productId;
        this.quantity = quantity;
        this.price = price;
        this.version = version;
    }

    public static CartItemBuilder builder() {
        return new CartItemBuilder();
    }

    public static class CartItemBuilder {
        private Long id;
        private String userId;
        private String productId;
        private int quantity;
        private BigDecimal price;
        private Long version = 0L;

        public CartItemBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public CartItemBuilder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public CartItemBuilder productId(String productId) {
            this.productId = productId;
            return this;
        }

        public CartItemBuilder quantity(int quantity) {
            this.quantity = quantity;
            return this;
        }

        public CartItemBuilder price(BigDecimal price) {
            this.price = price;
            return this;
        }

        public CartItemBuilder version(Long version) {
            this.version = version == null ? 0L : version;
            return this;
        }

        public CartItem build() {
            CartItem c = new CartItem(id, userId, productId, quantity, price == null ? BigDecimal.ZERO : price, version == null ? 0L : version);
            return c;
        }
    }

    @PrePersist
    public void prePersist() {
        if (version == null) {
            version = 0L;
        }
        if (price == null) {
            price = BigDecimal.ZERO;
        }
    }

    public BigDecimal getTotalPrice() {
        BigDecimal unitPrice = price == null ? BigDecimal.ZERO : price;
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

}
