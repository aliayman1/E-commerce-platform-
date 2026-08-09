package com.microservices.pro.order;

import com.microservices.pro.order.events.OrderPlacedEvent;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
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
    private final PaymentClient paymentClient;

    public OrderService(KafkaTemplate<String, Object> kafkaTemplate, OrderRepository orderRepository,
                        PaymentClient paymentClient) {
        this.kafkaTemplate = kafkaTemplate;
        this.orderRepository = orderRepository;
        this.paymentClient = paymentClient;
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

    // Synchronous payment path, kept alongside the Kafka saga above
    // circuit breaker needs a real outbound HTTP call to protect
    @CircuitBreaker(name = "paymentService", fallbackMethod = "paymentFallback")
    public PaymentOrderResponse createOrderWithPayment(OrderRequest request) {
        PaymentResponse payment = paymentClient.processPayment(new PaymentRequest(request.amount()));
        return new PaymentOrderResponse("CONFIRMED", payment.transactionId());
    }

    public PaymentOrderResponse paymentFallback(OrderRequest request, Throwable ex) {
        log.warn("[FALLBACK] Payment unavailable, returning PENDING. Reason: {}", ex.getMessage());
        return new PaymentOrderResponse("PENDING", null);
    }

    public OrderStatusResponse getStatus(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        return new OrderStatusResponse(order.getOrderId(), order.getStatus());
    }
}