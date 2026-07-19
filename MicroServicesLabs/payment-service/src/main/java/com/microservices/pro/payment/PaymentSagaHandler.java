package com.microservices.pro.payment;

import com.microservices.pro.payment.events.InventoryReservedEvent;
import com.microservices.pro.payment.events.PaymentCompletedEvent;
import com.microservices.pro.payment.events.PaymentFailedEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class PaymentSagaHandler {

    private static final Logger log = LoggerFactory.getLogger(PaymentSagaHandler.class);

    private final PaymentService paymentService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public PaymentSagaHandler(PaymentService paymentService, KafkaTemplate<String, Object> kafkaTemplate) {
        this.paymentService = paymentService;
        this.kafkaTemplate = kafkaTemplate;
    }

    // STEP 3 of Saga: Process payment when inventory reserved
    @KafkaListener(topics = "inventory-events", groupId = "payment-service")
    public void handleInventoryReserved(ConsumerRecord<String, Object> record) {
        Object event = record.value();
        if (!(event instanceof InventoryReservedEvent reserved)) {
            return; // ignore other events on this topic (e.g. InventoryReleased)
        }

        log.info("[SAGA] Processing payment for order: {}", reserved.orderId());
        try {
            String txId = paymentService.processPayment(reserved.orderId());
            kafkaTemplate.send("payment-events", reserved.orderId(),
                    new PaymentCompletedEvent(reserved.orderId(), txId));
            log.info("[SAGA] Payment COMPLETED for order: {} ✅", reserved.orderId());
        } catch (PaymentException e) {
            kafkaTemplate.send("payment-events", reserved.orderId(),
                    new PaymentFailedEvent(reserved.orderId(), e.getMessage()));
            log.warn("[SAGA] Payment FAILED for order: {} — triggering compensation", reserved.orderId());
        }
    }
}