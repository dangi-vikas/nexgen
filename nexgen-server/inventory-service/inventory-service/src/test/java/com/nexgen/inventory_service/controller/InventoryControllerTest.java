package com.nexgen.inventory_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexgen.inventory_service.entity.InventoryItem;
import com.nexgen.inventory_service.exception.DuplicateItemException;
import com.nexgen.inventory_service.exception.ItemNotFoundException;
import com.nexgen.inventory_service.service.InventoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InventoryController.class)
@WithMockUser(username = "testuser", roles = {"USER"})
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InventoryService inventoryService;

    @Autowired
    private ObjectMapper objectMapper;

    InventoryItem sampleItem() {
        return InventoryItem.builder()
                .skuCode("ABC123")
                .name("Test Item")
                .quantity(10)
                .build();
    }

    @Test
    @DisplayName("✅ Create Item - Success")
    void createItem_Success() throws Exception {
        InventoryItem item = sampleItem();

        Mockito.when(inventoryService.createItem(any())).thenReturn(item);

        mockMvc.perform(post("/api/v1/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(item))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.skuCode").value("ABC123"));
    }

    @Test
    @DisplayName("❌ Create Item - Duplicate Input")
    void createItem_InvalidInput() throws Exception {
        InventoryItem item = new InventoryItem();
        Mockito.when(inventoryService.createItem(eq(item)))
                .thenThrow(new DuplicateItemException("Item with SKU already exists: XYZ999"));

        mockMvc.perform(post("/api/v1/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(item))
                        .with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Duplicate Item Error"))
                .andExpect(jsonPath("$.message").value("Item with SKU already exists: XYZ999"));
    }

    @Test
    @DisplayName("✅ Update Item - Success")
    void updateItem_Success() throws Exception {
        InventoryItem item = sampleItem();
        Mockito.when(inventoryService.updateItem(eq("ABC123"), any())).thenReturn(item);

        mockMvc.perform(put("/api/v1/inventory/ABC123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(item))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.skuCode").value("ABC123"));
    }

    @Test
    @DisplayName("❌ Update Item - Not Found")
    void updateItem_NotFound() throws Exception {
        Mockito.when(inventoryService.updateItem(eq("XYZ999"), any()))
                .thenThrow(new ItemNotFoundException("Item not found with SKU: XYZ999"));

        mockMvc.perform(put("/api/v1/inventory/XYZ999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleItem()))
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Item not found"))
                .andExpect(jsonPath("$.message").value("Item not found with SKU: XYZ999"));
    }

    @Test
    @DisplayName("✅ Delete Item - Success")
    void deleteItem_Success() throws Exception {
        mockMvc.perform(delete("/api/v1/inventory/ABC123")
                    .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("❌ Delete Item - Not Found")
    void deleteItem_NotFound() throws Exception {
        Mockito.doThrow(new ItemNotFoundException("Item not found with SKU: XYZ999")).when(inventoryService).deleteItem("XYZ999");

        mockMvc.perform(delete("/api/v1/inventory/XYZ999")
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Item not found"))
                .andExpect(jsonPath("$.message").value("Item not found with SKU: XYZ999"));
    }

    @Test
    @DisplayName("✅ Get Item By SKU - Success")
    void getBySkuCode_Success() throws Exception {
        Mockito.when(inventoryService.getItemBySkuCode("ABC123")).thenReturn(sampleItem());

        mockMvc.perform(get("/api/v1/inventory/ABC123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.skuCode").value("ABC123"));
    }

    @Test
    @DisplayName("❌ Get Item By SKU - Not Found")
    void getBySkuCode_NotFound() throws Exception {
        Mockito.when(inventoryService.getItemBySkuCode("XYZ999"))
                .thenThrow(new ItemNotFoundException("Item not found with SKU: XYZ999"));

        mockMvc.perform(get("/api/v1/inventory/XYZ999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Item not found"))
                .andExpect(jsonPath("$.message").value("Item not found with SKU: XYZ999"));
    }

    @Test
    @DisplayName("✅ Get All Items - Success")
    void getAllItems_Success() throws Exception {
        Page<InventoryItem> page = new PageImpl<>(List.of(sampleItem()));
        Mockito.when(inventoryService.getAllItems(any(PageRequest.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/inventory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].skuCode").value("ABC123"));
    }
}
