package com.microservices.pro.notification;

import com.microservices.pro.notification.events.InventoryReleasedEvent;
import com.microservices.pro.notification.events.InventoryReservationFailedEvent;
import com.microservices.pro.notification.events.PaymentCompletedEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    // PAYMENT COMPLETED: send order confirmation email
    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2.0), // 1s → 2s → 4s
            dltTopicSuffix = ".DLT",
            autoCreateTopics = "false"
    )
    @KafkaListener(topics = "payment-events", groupId = "notification-service")
    public void handlePaymentCompleted(ConsumerRecord<String, Object> record) {
        if (!(record.value() instanceof PaymentCompletedEvent event)) {
            return; // ignore other events on this topic (e.g. PaymentFailed)
        }

        log.info("[NOTIFICATION] Sending confirmation email for order: {}", event.orderId());
        sendConfirmationEmail(event.orderId(), event.transactionId());
        log.info("[NOTIFICATION] ✅ Confirmation sent for order: {}", event.orderId());
    }

    // ORDER CANCELLED: send cancellation notification.
    // The platform never publishes an OrderCancelledEvent — on inventory-events the
    // cancellation signals are InventoryReservationFailed (out of stock) and
    // InventoryReleased (compensation after a failed payment).
    @RetryableTopic(attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            dltTopicSuffix = ".DLT")
    @KafkaListener(topics = "inventory-events", groupId = "notification-service-cancel")
    public void handleOrderCancelled(ConsumerRecord<String, Object> record) {
        String orderId;
        String reason;

        if (record.value() instanceof InventoryReservationFailedEvent failed) {
            orderId = failed.orderId();
            reason = failed.reason();
        } else if (record.value() instanceof InventoryReleasedEvent released) {
            orderId = released.orderId();
            reason = "Payment failed — inventory released";
        } else {
            return; // ignore other events on this topic (e.g. InventoryReserved)
        }

        log.info("[NOTIFICATION] Sending cancellation notification for order: {}", orderId);
        sendCancellationNotification(orderId, reason);
    }

    // DLT HANDLER: log events that exhausted all retries
    @DltHandler
    public void handleDlt(ConsumerRecord<String, Object> record,
                          @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.error("[NOTIFICATION] ❌ DLT: Event from topic '{}' exhausted all retries. "
                + "Manual intervention required. Event: {}", topic, record.value());
        // In production: send to monitoring/alerting (PagerDuty, Slack, etc.)
    }

    private void sendConfirmationEmail(String orderId, String txId) {
        log.info("[EMAIL] Order {} confirmed. Transaction: {}", orderId, txId);
        // DEV ONLY: simulate sending. Production: use JavaMailSender or an email SDK.
    }

    private void sendCancellationNotification(String orderId, String reason) {
        log.info("[EMAIL] Order {} cancelled. Reason: {}", orderId, reason);
    }
}