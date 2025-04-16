package com.nexgen.inventory_service.service;

import com.nexgen.inventory_service.entity.InventoryItem;
import com.nexgen.inventory_service.repository.InventoryRepository;
import com.nexgen.inventory_service.dto.InventoryEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InventoryServiceImplTest {

    @Mock
    private InventoryRepository repository;

    @Mock
    private KafkaInventoryProducer kafkaProducer;

    @InjectMocks
    private InventoryServiceImpl inventoryService;

    private InventoryItem item;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        item = new InventoryItem();
        item.setId(1L);
        item.setSkuCode("ABC123");
        item.setName("Test Item");
        item.setQuantity(10);
    }

    @Test
    void testCreateItem_ShouldSaveAndPublishEvent() {
        when(repository.save(any())).thenReturn(item);

        InventoryItem result = inventoryService.createItem(item);

        assertNotNull(result);
        assertEquals("ABC123", result.getSkuCode());
        verify(repository, times(1)).save(item);
        verify(kafkaProducer, times(1)).sendInventoryEvent(any(InventoryEvent.class));
    }

    @Test
    void testGetItemBySkuCode_WhenExists() {
        when(repository.findBySkuCode("ABC123")).thenReturn(Optional.of(item));

        InventoryItem found = inventoryService.getItemBySkuCode("ABC123");

        assertEquals("ABC123", found.getSkuCode());
        verify(repository).findBySkuCode("ABC123");
    }

    @Test
    void testGetItemBySkuCode_WhenNotFound() {
        when(repository.findBySkuCode("XYZ")).thenReturn(Optional.empty());

        Exception ex = assertThrows(RuntimeException.class, () -> {
            inventoryService.getItemBySkuCode("XYZ");
        });

        assertTrue(ex.getMessage().contains("Item not found"));
    }

    @Test
    void testUpdateItem_ShouldUpdateAndPublishEvents() {
        InventoryItem update = new InventoryItem();
        update.setName("Updated");
        update.setQuantity(0);

        when(repository.findBySkuCode("ABC123")).thenReturn(Optional.of(item));
        when(repository.save(any())).thenReturn(item);

        InventoryItem updated = inventoryService.updateItem("ABC123", update);

        assertEquals("Updated", updated.getName());
        assertEquals(0, updated.getQuantity());
        verify(kafkaProducer, times(2)).sendInventoryEvent(any(InventoryEvent.class)); // UPDATED + OUT_OF_STOCK
    }

    @Test
    void testDeleteItem_ShouldDelete() {
        when(repository.findBySkuCode("ABC123")).thenReturn(Optional.of(item));
        doNothing().when(repository).delete(item);

        inventoryService.deleteItem("ABC123");

        verify(repository).delete(item);
    }

    @Test
    void testGetAllItems_ShouldReturnPage() {
        List<InventoryItem> items = List.of(item);
        Pageable pageable = PageRequest.of(0, 5);
        when(repository.findAll(pageable)).thenReturn(new PageImpl<>(items));

        var result = inventoryService.getAllItems(pageable);

        assertEquals(1, result.getTotalElements());
    }
}
