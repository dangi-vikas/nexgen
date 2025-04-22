package com.nexgen.product_service.service;


import com.nexgen.product_service.config.RedisCacheUtil;
import com.nexgen.product_service.dto.ProductEvent;
import com.nexgen.product_service.dto.RedisPageWrapper;
import com.nexgen.product_service.entity.Product;
import com.nexgen.product_service.exception.DuplicateProductException;
import com.nexgen.product_service.exception.ProductNotFoundException;
import com.nexgen.product_service.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductServiceTest {

    @Mock
    private ProductRepository repository;

    @Mock
    private KafkaProducerService producer;

    @Mock
    private RedisCacheUtil redisCacheUtil;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product product;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);

        product = Product.builder()
                .skuCode("P1001")
                .name("TestProduct")
                .description("Description")
                .price(100.00)
                .quantity(10)
                .build();
    }

    @Test
    void testCreateProductSuccess() {
        when(repository.findBySkuCode("P1001")).thenReturn(Optional.empty());
        when(repository.save(any(Product.class))).thenReturn(product);

        Product saved = productService.createProduct(product);

        assertNotNull(saved);
        assertEquals("P1001", saved.getSkuCode());
        verify(producer).sendProductCreatedEvent(any(ProductEvent.class));
    }

    @Test
    void testCreateProductDuplicate() {
        when(repository.findBySkuCode("P1001")).thenReturn(Optional.of(product));

        assertThrows(DuplicateProductException.class, () -> productService.createProduct(product));
    }

    @Test
    void testUpdateProductSuccess() {
        when(repository.findBySkuCode("P1001")).thenReturn(Optional.of(product));
        when(repository.save(any(Product.class))).thenReturn(product);

        Product updated = productService.updateProduct("P1001", product);

        assertNotNull(updated);
        verify(producer).sendProductStockUpdatedEvent(any(ProductEvent.class));
    }

    @Test
    void testUpdateProductNotFound() {
        when(repository.findBySkuCode("P1001")).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> productService.updateProduct("P1001", product));
    }

    @Test
    void testDeleteProductSuccess() {
        when(repository.findBySkuCode("P1001")).thenReturn(Optional.of(product));
        doNothing().when(repository).delete(any(Product.class));

        assertDoesNotThrow(() -> productService.deleteProduct("P1001"));
        verify(producer).sendProductDeletedEvent(any(ProductEvent.class));
    }

    @Test
    void testDeleteProductNotFound() {
        when(repository.findBySkuCode("P1001")).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> productService.deleteProduct("P1001"));
    }

    @Test
    void testGetProductBySkuCodeFound() {
        when(repository.findBySkuCode("P1001")).thenReturn(Optional.of(product));

        Product found = productService.getProductBySkuCode("P1001");

        assertNotNull(found);
        assertEquals("P1001", found.getSkuCode());
    }

    @Test
    void testGetProductBySkuCodeNotFound() {
        when(repository.findBySkuCode("P1001")).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> productService.getProductBySkuCode("P1001"));
    }

    @Test
    void testGetAllProducts() {
        List<Product> products = List.of(product);
        Page<Product> page = new PageImpl<>(products);
        Pageable pageable = PageRequest.of(0, 10);

        when(repository.findAll(pageable)).thenReturn(page);

        RedisPageWrapper<Product> wrapper = productService.getAllProducts(pageable);

        assertNotNull(wrapper);
        assertEquals(1, wrapper.getContent().size());
        assertEquals("P1001", wrapper.getContent().get(0).getSkuCode());
    }
}
