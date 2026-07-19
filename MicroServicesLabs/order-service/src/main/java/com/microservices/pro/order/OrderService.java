package com.microservices.pro.order;

import com.microservices.pro.order.events.OrderPlacedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final OrderRepository orderRepository;

    public OrderService(KafkaTemplate<String, Object> kafkaTemplate, OrderRepository orderRepository) {
        this.kafkaTemplate = kafkaTemplate;
        this.orderRepository = orderRepository;
    }

    public OrderResponse createOrder(OrderRequest request) {
        // Step 1: Create order in PENDING state (local transaction)
        Order order = new Order(UUID.randomUUID().toString(), request.productId(),
                request.quantity(), request.amount(), request.customerId(), OrderStatus.PENDING);
        orderRepository.save(order);

        // Step 2: Publish event to start the Saga (async — no waiting)
        kafkaTemplate.send("order-events", order.getOrderId(),
                new OrderPlacedEvent(order.getOrderId(), request.productId(),
                        request.quantity(), request.amount(), request.customerId()));

        log.info("[SAGA] Order {} PENDING — OrderPlaced published", order.getOrderId());

        // Return immediately — client gets PENDING, not final state
        return new OrderResponse(order.getOrderId(), OrderStatus.PENDING, "Order received — processing...");
    }

    public OrderStatusResponse getStatus(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        return new OrderStatusResponse(order.getOrderId(), order.getStatus());
    }
}