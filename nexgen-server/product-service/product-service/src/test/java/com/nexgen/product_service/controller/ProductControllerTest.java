package com.nexgen.product_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexgen.product_service.config.PageCacheUtil;
import com.nexgen.product_service.config.SecurityConfig;
import com.nexgen.product_service.dto.RedisPageWrapper;
import com.nexgen.product_service.entity.Product;
import com.nexgen.product_service.exception.ProductNotFoundException;
import com.nexgen.product_service.service.ProductService;
import com.nexgen.product_service.support.JwtTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "jwt.secret=01234567890123456789012345678901",
        "jwt.issuer=nexgen-user-service",
        "jwt.audience=nexgen-api"
})
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @Autowired
    private ObjectMapper objectMapper;

    private Product product;

    @BeforeEach
    void setUp() {
        product = Product.builder()
                .skuCode("SKU123")
                .name("Test Product")
                .description("Description")
                .price(java.math.BigDecimal.valueOf(199.99))
                .quantity(10)
                .build();
    }

    // POST /create
    @Test
    void createProduct_ReturnsCreated() throws Exception {
        when(productService.createProduct(any(Product.class))).thenReturn(product);

        mockMvc.perform(post("/api/v1/products")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(product)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.skuCode").value("SKU123"));
    }

    @Test
    void createProductWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(product)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createProductWithUserRoleIsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(product)))
                .andExpect(status().isForbidden());
    }

    // PUT /update
    @Test
    void updateProduct_ReturnsUpdated() throws Exception {
        when(productService.updateProduct(eq("SKU123"), any(Product.class))).thenReturn(product);

        mockMvc.perform(put("/api/v1/products/SKU123")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(product)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Product"));
    }

    // DELETE
    @Test
    void deleteProduct_ReturnsNoContent() throws Exception {
        doNothing().when(productService).deleteProduct("SKU123");

        mockMvc.perform(delete("/api/v1/products/SKU123")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNoContent());
    }

    // GET by SKU
    @Test
    void getBySkuCode_ReturnsProduct() throws Exception {
        when(productService.getProductBySkuCode("SKU123")).thenReturn(product);

        mockMvc.perform(get("/api/v1/products/SKU123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.skuCode").value("SKU123"));
    }

    @Test
    void getBySkuCode_ThrowsProductNotFound() throws Exception {
        when(productService.getProductBySkuCode("SKU404")).thenThrow(new ProductNotFoundException("SKU404"));

        mockMvc.perform(get("/api/v1/products/SKU404"))
                .andExpect(status().isNotFound());
    }

    // GET all products
    @Test
    void getAllProducts_ReturnsPage() throws Exception {
        Page<Product> page = new PageImpl<>(List.of(product));
        RedisPageWrapper<Product> wrapper = PageCacheUtil.wrap(page);

        when(productService.getAllProducts(any())).thenReturn(wrapper);

        mockMvc.perform(get("/api/v1/products?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].skuCode").value("SKU123"));
    }

    @Test
    void realAdminTokenCanCreateProduct() throws Exception {
        when(productService.createProduct(any(Product.class))).thenReturn(product);

        mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + JwtTestSupport.accessToken("admin", "ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(product)))
                .andExpect(status().isOk());
    }

    @Test
    void realUserTokenCannotCreateProduct() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + JwtTestSupport.accessToken("user123", "ROLE_USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(product)))
                .andExpect(status().isForbidden());
    }

    @Test
    void refreshTokenIsRejectedOnProductWrite() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + JwtTestSupport.refreshToken("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(product)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenWithWrongAudienceIsRejectedOnProductWrite() throws Exception {
        String token = JwtTestSupport.token("nexgen-user-service", "some-other-api",
                "access", "admin", "ROLE_ADMIN");

        mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(product)))
                .andExpect(status().isUnauthorized());
    }
}
