package com.microservices.pro.inventory;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InventoryService {

    private record Reservation(String productId, int quantity) {}

    // In-memory store (PostgreSQL added as homework)
    // In-memory: PROD-001 (100 units), PROD-002 (5 units), PROD-003 (0 units — out of stock)
    private final Map<String, StockItem> stock = new ConcurrentHashMap<>(Map.of(
            "PROD-001", new StockItem("PROD-001", 100, 0),
            "PROD-002", new StockItem("PROD-002", 5, 0),
            "PROD-003", new StockItem("PROD-003", 0, 0) // out of stock
    ));

    private final Map<String, Reservation> reservations = new ConcurrentHashMap<>();

    public StockCheckResponse checkStock(String productId, int requestedQty) {
        StockItem item = stock.getOrDefault(productId,
                new StockItem(productId, 0, 0));
        boolean available = item.hasStock(requestedQty);
        return new StockCheckResponse(productId, requestedQty,
                available, item.availableQuantity() - item.reservedQuantity());
    }

    // Saga step: reserve stock for an order (local transaction)
    public synchronized void reserveStock(String productId, int quantity, String orderId) {
        StockItem item = stock.getOrDefault(productId, new StockItem(productId, 0, 0));
        if (!item.hasStock(quantity)) {
            throw new InsufficientStockException("Insufficient stock for product " + productId);
        }
        stock.put(productId, new StockItem(productId, item.availableQuantity(), item.reservedQuantity() + quantity));
        reservations.put(orderId, new Reservation(productId, quantity));
    }

    // Compensation: release a previously reserved quantity. Idempotent — a second
    // release for the same order (already released) is a no-op.
    public synchronized void releaseStock(String orderId) {
        Reservation reservation = reservations.remove(orderId);
        if (reservation == null) {
            return;
        }
        StockItem item = stock.get(reservation.productId());
        stock.put(reservation.productId(), new StockItem(reservation.productId(),
                item.availableQuantity(), item.reservedQuantity() - reservation.quantity()));
    }
}