package com.nexgen.product_service.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.io.Serializable;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Product name must not be blank")
    @Size(min = 2, max = 100, message = "Product name must be between 2 and 100 characters")
    private String name;

    @NotBlank(message = "SKU code must not be blank")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "SKU code can only contain alphanumeric characters, underscores or hyphens")
    private String skuCode;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    private Double price;

    @NotNull(message = "Quantity is required")
    @Min(value = 0, message = "Quantity must be zero or more")
    private Integer quantity;

    @Size(max = 500, message = "Description can't exceed 500 characters")
    private String description;
}
