package com.microservices.pro.inventory;

import com.microservices.pro.inventory.events.InventoryReleasedEvent;
import com.microservices.pro.inventory.events.InventoryReservationFailedEvent;
import com.microservices.pro.inventory.events.InventoryReservedEvent;
import com.microservices.pro.inventory.events.OrderPlacedEvent;
import com.microservices.pro.inventory.events.PaymentFailedEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class InventorySagaHandler {

    private static final Logger log = LoggerFactory.getLogger(InventorySagaHandler.class);

    private final InventoryService inventoryService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public InventorySagaHandler(InventoryService inventoryService, KafkaTemplate<String, Object> kafkaTemplate) {
        this.inventoryService = inventoryService;
        this.kafkaTemplate = kafkaTemplate;
    }

    // STEP 2 of Saga: Reserve inventory when order is placed
    @KafkaListener(topics = "order-events", groupId = "inventory-service")
    public void handleOrderPlaced(OrderPlacedEvent event) {
        log.info("[SAGA] Handling OrderPlaced for order: {}", event.orderId());
        try {
            inventoryService.reserveStock(event.productId(), event.quantity(), event.orderId());
            // Success: publish InventoryReserved
            kafkaTemplate.send("inventory-events", event.orderId(),
                    new InventoryReservedEvent(event.orderId(), event.productId(), event.quantity()));
            log.info("[SAGA] Inventory reserved for order: {} ✅", event.orderId());
        } catch (InsufficientStockException e) {
            // Failure: publish InventoryReservationFailed (starts compensation)
            kafkaTemplate.send("inventory-events", event.orderId(),
                    new InventoryReservationFailedEvent(event.orderId(), e.getMessage()));
            log.warn("[SAGA] Inventory reservation FAILED for order: {} — {}", event.orderId(), e.getMessage());
        }
    }

    // COMPENSATION: Release inventory when payment fails
    @KafkaListener(topics = "payment-events", groupId = "inventory-compensation")
    public void handlePaymentFailed(ConsumerRecord<String, Object> record) {
        if (record.value() instanceof PaymentFailedEvent failed) {
            inventoryService.releaseStock(failed.orderId()); // undo reservation
            kafkaTemplate.send("inventory-events", failed.orderId(),
                    new InventoryReleasedEvent(failed.orderId()));
            log.info("[SAGA] COMPENSATION: Inventory released for order: {} ✅", failed.orderId());
        }
    }
}