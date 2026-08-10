package com.microservices.pro.order.saga;

import com.microservices.pro.order.OrderRepository;
import com.microservices.pro.order.OrderRequest;
import com.microservices.pro.order.OrderResponse;
import com.microservices.pro.order.OrderStatus;
import com.microservices.pro.order.events.InventoryReleasedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderSagaOrchestratorTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private final OrderRepository orderRepository = new OrderRepository();
    private OrderSagaOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        orchestrator = new OrderSagaOrchestrator(kafkaTemplate, orderRepository);
    }

    private String startSaga() {
        OrderResponse response = orchestrator.startSaga(
                new OrderRequest("PROD-001", 2, new BigDecimal("600.00"), "cust-1"));
        return response.orderId();
    }

    private OrderStatus statusOf(String orderId) {
        return orderRepository.findById(orderId).orElseThrow().getStatus();
    }

    @Test
    void startSaga_savesPendingOrderAndSendsReserveInventoryCommand() {
        OrderResponse response = orchestrator.startSaga(
                new OrderRequest("PROD-001", 2, new BigDecimal("600.00"), "cust-1"));

        assertEquals(OrderStatus.PENDING, response.status());
        assertEquals(OrderStatus.PENDING, statusOf(response.orderId()));
        assertEquals(SagaState.INVENTORY_RESERVING, orchestrator.getSagaState(response.orderId()));

        ArgumentCaptor<Object> command = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq("saga-commands"), eq(response.orderId()), command.capture());

        ReserveInventoryCommand sent = assertInstanceOf(ReserveInventoryCommand.class, command.getValue());
        assertEquals(response.orderId(), sent.orderId());
        assertEquals("PROD-001", sent.productId());
        assertEquals(2, sent.quantity());
    }

    @Test
    void handleInventoryResult_success_sendsProcessPaymentCommand() {
        String orderId = startSaga();

        orchestrator.handleInventoryResult(new InventoryResultEvent(orderId, true));

        assertEquals(SagaState.PAYMENT_PROCESSING, orchestrator.getSagaState(orderId));

        ArgumentCaptor<Object> command = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate, times(2))
                .send(eq("saga-commands"), eq(orderId), command.capture());

        ProcessPaymentCommand sent = assertInstanceOf(ProcessPaymentCommand.class, command.getAllValues().get(1));
        assertEquals(orderId, sent.orderId());
        assertEquals(new BigDecimal("600.00"), sent.amount());
    }

    @Test
    void handleInventoryResult_failure_cancelsOrderWithoutCompensation() {
        String orderId = startSaga();

        orchestrator.handleInventoryResult(new InventoryResultEvent(orderId, false));

        assertEquals(OrderStatus.CANCELLED, statusOf(orderId));
        assertNull(orchestrator.getSagaState(orderId));
        verify(kafkaTemplate, never()).send(eq("saga-commands"), eq(orderId), any(ReleaseInventoryCommand.class));
    }

    @Test
    void handlePaymentResult_success_confirmsOrderAndCompletesSaga() {
        String orderId = startSaga();
        orchestrator.handleInventoryResult(new InventoryResultEvent(orderId, true));

        orchestrator.handlePaymentResult(new PaymentResultEvent(orderId, true));

        assertEquals(OrderStatus.CONFIRMED, statusOf(orderId));
        assertNull(orchestrator.getSagaState(orderId));
    }

    @Test
    void handlePaymentResult_failure_sendsReleaseInventoryCommand() {
        String orderId = startSaga();
        orchestrator.handleInventoryResult(new InventoryResultEvent(orderId, true));

        orchestrator.handlePaymentResult(new PaymentResultEvent(orderId, false));

        assertEquals(SagaState.INVENTORY_RELEASING, orchestrator.getSagaState(orderId));
        verify(kafkaTemplate).send(eq("saga-commands"), eq(orderId), any(ReleaseInventoryCommand.class));
    }

    @Test
    void handleInventoryReleased_cancelsOrderAndEndsSaga() {
        String orderId = startSaga();
        orchestrator.handleInventoryResult(new InventoryResultEvent(orderId, true));
        orchestrator.handlePaymentResult(new PaymentResultEvent(orderId, false));

        orchestrator.handleInventoryReleased(new InventoryReleasedEvent(orderId));

        assertEquals(OrderStatus.CANCELLED, statusOf(orderId));
        assertNull(orchestrator.getSagaState(orderId));
    }

    @Test
    void handleInventoryResult_ignoresDuplicate_whenStateAlreadyAdvanced() {
        String orderId = startSaga();
        orchestrator.handleInventoryResult(new InventoryResultEvent(orderId, true));

        orchestrator.handleInventoryResult(new InventoryResultEvent(orderId, true));

        assertEquals(SagaState.PAYMENT_PROCESSING, orchestrator.getSagaState(orderId));
        verify(kafkaTemplate, times(2)).send(eq("saga-commands"), eq(orderId), any());
    }

    @Test
    void handlePaymentResult_ignored_whenSagaNotAwaitingPayment() {
        String orderId = startSaga();

        orchestrator.handlePaymentResult(new PaymentResultEvent(orderId, false));

        assertEquals(SagaState.INVENTORY_RESERVING, orchestrator.getSagaState(orderId));
        verify(kafkaTemplate, never()).send(eq("saga-commands"), eq(orderId), any(ReleaseInventoryCommand.class));
    }
}