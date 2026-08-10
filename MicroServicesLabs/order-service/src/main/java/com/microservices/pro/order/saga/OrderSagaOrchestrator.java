package com.microservices.pro.order.saga;

import com.microservices.pro.order.Order;
import com.microservices.pro.order.OrderRepository;
import com.microservices.pro.order.OrderRequest;
import com.microservices.pro.order.OrderResponse;
import com.microservices.pro.order.OrderStatus;
import com.microservices.pro.order.events.InventoryReleasedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OrderSagaOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(OrderSagaOrchestrator.class);

    // DEV ONLY: in-memory state - lost on restart.
    // Production: persist SagaState to DB (see Homework §8).
    private final Map<String, SagaState> sagaStates = new ConcurrentHashMap<>();

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final OrderRepository orderRepository;

    public OrderSagaOrchestrator(KafkaTemplate<String, Object> kafkaTemplate, OrderRepository orderRepository) {
        this.kafkaTemplate = kafkaTemplate;
        this.orderRepository = orderRepository;
    }

    public OrderResponse startSaga(OrderRequest request) {
        String orderId = UUID.randomUUID().toString();

        Order order = new Order(orderId, request.productId(), request.quantity(),
                request.amount(), request.customerId(), OrderStatus.PENDING);
        orderRepository.save(order);

        sagaStates.put(orderId, SagaState.STARTED);
        log.info("[SAGA] {} null → {}", orderId, SagaState.STARTED);

        kafkaTemplate.send("saga-commands", orderId,
                new ReserveInventoryCommand(orderId, request.productId(), request.quantity()));

        transition(orderId, SagaState.INVENTORY_RESERVING);

        return new OrderResponse(orderId, OrderStatus.PENDING, "Order received — processing...");
    }

    @KafkaListener(topics = "saga-results", groupId = "orchestrator-inventory")
    public void handleInventoryResult(InventoryResultEvent event) {
        SagaState current = sagaStates.get(event.orderId());
        if (current != SagaState.INVENTORY_RESERVING) {
            log.warn("[SAGA] Unexpected state {} for order {}", current, event.orderId());
            return;
        }

        if (event.success()) {
            transition(event.orderId(), SagaState.INVENTORY_RESERVED);
            kafkaTemplate.send("saga-commands", event.orderId(),
                    new ProcessPaymentCommand(event.orderId(), getOrderAmount(event.orderId())));
            transition(event.orderId(), SagaState.PAYMENT_PROCESSING);
        } else {
            transition(event.orderId(), SagaState.INVENTORY_RESERVE_FAILED);
            updateOrderStatus(event.orderId(), OrderStatus.CANCELLED);
            sagaStates.remove(event.orderId());
        }
    }

    @KafkaListener(topics = "saga-results", groupId = "orchestrator-payment")
    public void handlePaymentResult(PaymentResultEvent event) {
        SagaState current = sagaStates.get(event.orderId());
        if (current != SagaState.PAYMENT_PROCESSING) {
            return;
        }

        if (event.success()) {
            transition(event.orderId(), SagaState.COMPLETED);
            updateOrderStatus(event.orderId(), OrderStatus.CONFIRMED);
            sagaStates.remove(event.orderId());
            log.info("[SAGA] ✅ Order {} CONFIRMED", event.orderId());
        } else {
            transition(event.orderId(), SagaState.PAYMENT_FAILED);
            kafkaTemplate.send("saga-commands", event.orderId(),
                    new ReleaseInventoryCommand(event.orderId()));
            transition(event.orderId(), SagaState.INVENTORY_RELEASING);
        }
    }

    @KafkaListener(topics = "saga-results", groupId = "orchestrator-compensation")
    public void handleInventoryReleased(InventoryReleasedEvent event) {
        transition(event.orderId(), SagaState.CANCELLED);
        updateOrderStatus(event.orderId(), OrderStatus.CANCELLED);
        sagaStates.remove(event.orderId());
        log.info("[SAGA] ✅ Order {} CANCELLED — compensation complete", event.orderId());
    }

    private void transition(String orderId, SagaState newState) {
        SagaState old = sagaStates.put(orderId, newState);
        log.info("[SAGA] {} {} → {}", orderId, old, newState);
    }

    private BigDecimal getOrderAmount(String orderId) {
        return orderRepository.findById(orderId).map(Order::getAmount).orElse(BigDecimal.ZERO);
    }

    private void updateOrderStatus(String orderId, OrderStatus status) {
        orderRepository.findById(orderId).ifPresent(order -> order.setStatus(status));
    }

    SagaState getSagaState(String orderId) {
        return sagaStates.get(orderId);
    }
}