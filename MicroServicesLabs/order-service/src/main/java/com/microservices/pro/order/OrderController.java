package com.microservices.pro.order;

import com.microservices.pro.order.saga.OrderSagaOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    private final OrderService orderService;
    private final OrderSagaOrchestrator sagaOrchestrator;
    private final InventoryClient inventoryClient;

    public OrderController(OrderService orderService, OrderSagaOrchestrator sagaOrchestrator,
                           InventoryClient inventoryClient) {
        this.orderService = orderService;
        this.sagaOrchestrator = sagaOrchestrator;
        this.inventoryClient = inventoryClient;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@RequestBody OrderRequest request) {
        return ResponseEntity.ok(orderService.createOrder(request));
    }

    @PostMapping("/saga")
    public ResponseEntity<OrderResponse> startSaga(@RequestBody OrderRequest request) {
        return ResponseEntity.ok(sagaOrchestrator.startSaga(request));
    }

    @GetMapping("/{orderId}/status")
    public ResponseEntity<OrderStatusResponse> getOrderStatus(@PathVariable String orderId) {
        return ResponseEntity.ok(orderService.getStatus(orderId));
    }

    @GetMapping("/stock-check")
    public ResponseEntity<StockCheckResponse> checkStock(
            @RequestParam String productId,
            @RequestParam int quantity,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        log.info("[AUTH] Stock check requested by X-User-Id={}, X-User-Role={}", userId, userRole);
        return ResponseEntity.ok(inventoryClient.checkStock(productId, quantity));
    }
}
