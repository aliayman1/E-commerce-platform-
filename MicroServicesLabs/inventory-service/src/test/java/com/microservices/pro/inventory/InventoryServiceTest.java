package com.microservices.pro.inventory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventoryServiceTest {

    private final InventoryService inventoryService = new InventoryService();

    @Test
    void checkStock_returnsAvailable_whenEnoughStock() {
        StockCheckResponse response = inventoryService.checkStock("PROD-001", 5);

        assertTrue(response.available());
        assertEquals(100, response.remainingStock());
    }

    @Test
    void checkStock_returnsUnavailable_whenOutOfStock() {
        StockCheckResponse response = inventoryService.checkStock("PROD-003", 1);

        assertFalse(response.available());
        assertEquals(0, response.remainingStock());
    }

    @Test
    void checkStock_returnsUnavailable_whenProductUnknown() {
        StockCheckResponse response = inventoryService.checkStock("PROD-999", 1);

        assertFalse(response.available());
    }
}