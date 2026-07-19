package com.microservices.pro.order;

import com.microservices.pro.order.events.InventoryReleasedEvent;
import com.microservices.pro.order.events.PaymentCompletedEvent;
import com.microservices.pro.order.events.PaymentFailedEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class OrderSagaEventHandler {

    private static final Logger log = LoggerFactory.getLogger(OrderSagaEventHandler.class);

    private final OrderRepository orderRepository;

    public OrderSagaEventHandler(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    // Listen for Saga completion events
    @KafkaListener(topics = "payment-events", groupId = "order-service")
    public void handlePaymentEvent(ConsumerRecord<String, Object> record) {
        Object event = record.value();
        if (event instanceof PaymentCompletedEvent completed) {
            orderRepository.findById(completed.orderId()).ifPresent(order -> {
                order.setStatus(OrderStatus.CONFIRMED);
                log.info("[SAGA] Order {} CONFIRMED ✅", completed.orderId());
            });
        } else if (event instanceof PaymentFailedEvent failed) {
            orderRepository.findById(failed.orderId()).ifPresent(order -> {
                order.setStatus(OrderStatus.PAYMENT_FAILED);
                log.warn("[SAGA] Order {} payment failed, waiting for inventory release...", failed.orderId());
            });
        }
    }

    // Listen for compensation completion (inventory-events also carries InventoryReserved, which we ignore here)
    @KafkaListener(topics = "inventory-events", groupId = "order-service-cancel")
    public void handleInventoryReleased(ConsumerRecord<String, Object> record) {
        if (record.value() instanceof InventoryReleasedEvent released) {
            orderRepository.findById(released.orderId()).ifPresent(order -> {
                if (order.getStatus() == OrderStatus.CANCELLED) {
                    return; // idempotency guard — avoid double compensation
                }
                order.setStatus(OrderStatus.CANCELLED);
                log.info("[SAGA] Order {} CANCELLED — inventory released ✅", released.orderId());
            });
        }
    }
}