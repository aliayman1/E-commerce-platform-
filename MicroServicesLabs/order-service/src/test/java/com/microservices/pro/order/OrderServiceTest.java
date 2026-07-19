package com.microservices.pro.order;

import com.microservices.pro.order.events.InventoryReleasedEvent;
import com.microservices.pro.order.events.OrderPlacedEvent;
import com.microservices.pro.order.events.PaymentCompletedEvent;
import com.microservices.pro.order.events.PaymentFailedEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private final OrderRepository orderRepository = new OrderRepository();
    private OrderService orderService;
    private OrderSagaEventHandler sagaEventHandler;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(kafkaTemplate, orderRepository);
        sagaEventHandler = new OrderSagaEventHandler(orderRepository);
    }

    private static ConsumerRecord<String, Object> record(String topic, String key, Object value) {
        return new ConsumerRecord<>(topic, 0, 0L, key, value);
    }

    @Test
    void createOrder_savesPendingOrderAndPublishesOrderPlaced() {
        OrderResponse response = orderService.createOrder(
                new OrderRequest("PROD-001", 3, BigDecimal.TEN, "cust-1"));

        assertEquals(OrderStatus.PENDING, response.status());
        verify(kafkaTemplate).send(eq("order-events"), eq(response.orderId()), any(OrderPlacedEvent.class));
    }

    @Test
    void getStatus_throws_whenOrderUnknown() {
        assertThrows(OrderNotFoundException.class, () -> orderService.getStatus("missing"));
    }

    @Test
    void handlePaymentEvent_confirmsOrder_onPaymentCompleted() {
        OrderResponse response = orderService.createOrder(
                new OrderRequest("PROD-001", 1, BigDecimal.TEN, "cust-1"));

        sagaEventHandler.handlePaymentEvent(
                record("payment-events", response.orderId(), new PaymentCompletedEvent(response.orderId(), "txn-1")));

        assertEquals(OrderStatus.CONFIRMED, orderService.getStatus(response.orderId()).status());
    }

    @Test
    void handlePaymentEvent_marksPaymentFailed_onPaymentFailed() {
        OrderResponse response = orderService.createOrder(
                new OrderRequest("PROD-001", 1, BigDecimal.TEN, "cust-1"));

        sagaEventHandler.handlePaymentEvent(
                record("payment-events", response.orderId(), new PaymentFailedEvent(response.orderId(), "Payment declined")));

        assertEquals(OrderStatus.PAYMENT_FAILED, orderService.getStatus(response.orderId()).status());
    }

    @Test
    void handleInventoryReleased_cancelsOrder_asCompensation() {
        OrderResponse response = orderService.createOrder(
                new OrderRequest("PROD-001", 1, BigDecimal.TEN, "cust-1"));
        sagaEventHandler.handlePaymentEvent(
                record("payment-events", response.orderId(), new PaymentFailedEvent(response.orderId(), "Payment declined")));

        sagaEventHandler.handleInventoryReleased(
                record("inventory-events", response.orderId(), new InventoryReleasedEvent(response.orderId())));

        assertEquals(OrderStatus.CANCELLED, orderService.getStatus(response.orderId()).status());
    }
}