package com.microservices.pro.inventory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @Test
    void reserveStock_reducesAvailableStock() {
        inventoryService.reserveStock("PROD-001", 5, "order-1");

        assertEquals(95, inventoryService.checkStock("PROD-001", 1).remainingStock());
    }

    @Test
    void reserveStock_throws_whenInsufficientStock() {
        assertThrows(InsufficientStockException.class,
                () -> inventoryService.reserveStock("PROD-002", 10, "order-2"));
    }

    @Test
    void releaseStock_restoresReservedQuantity() {
        inventoryService.reserveStock("PROD-001", 5, "order-3");

        inventoryService.releaseStock("order-3");

        assertEquals(100, inventoryService.checkStock("PROD-001", 1).remainingStock());
    }

    @Test
    void releaseStock_isIdempotent_whenCalledTwice() {
        inventoryService.reserveStock("PROD-001", 5, "order-4");
        inventoryService.releaseStock("order-4");

        inventoryService.releaseStock("order-4"); // second release should be a no-op

        assertEquals(100, inventoryService.checkStock("PROD-001", 1).remainingStock());
    }
}