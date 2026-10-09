package com.ecommerce.notification.service;

import com.ecommerce.notification.dto.NotificationEvent;
import com.ecommerce.notification.entity.NotificationLog;
import com.ecommerce.notification.entity.NotificationStatus;
import com.ecommerce.notification.repository.NotificationLogRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy");

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final NotificationLogRepository logRepository;

    @Value("${notification.mail-from}")
    private String mailFrom;

    @Value("${notification.override-recipient:}")
    private String overrideRecipient;

    @Value("${notification.default-email-domain}")
    private String defaultEmailDomain;

    /**
     * Render template + gửi email + ghi log. Nếu gửi lỗi sẽ ném lại exception
     * để RabbitMQ retry và cuối cùng đẩy vào Dead Letter Queue.
     */
    public void process(NotificationEvent event) {
        String type = event.getEventType();
        String template;
        String subject;
        if ("PAYMENT_SUCCESS".equals(type)) {
            template = "payment-success";
            subject = "Thanh toán thành công cho đơn hàng #" + event.getOrderId();
        } else if ("ORDER_CREATED".equals(type)) {
            template = "order-created";
            subject = "Xác nhận đặt hàng #" + event.getOrderId();
        } else {
            log.warn("Unknown notification eventType={}, skip", type);
            return;
        }

        String recipient = resolveEmail(event);

        try {
            Context ctx = new Context(new Locale("vi", "VN"));
            ctx.setVariable("orderId", event.getOrderId());
            ctx.setVariable("customer", event.getCustomerEmail() != null ? event.getCustomerEmail() : event.getUserId());
            ctx.setVariable("amount", formatMoney(event.getAmount()));
            ctx.setVariable("paymentMethod", event.getPaymentMethod());
            ctx.setVariable("bankCode", event.getBankCode());
            ctx.setVariable("transactionCode", event.getTransactionCode());
            ctx.setVariable("providerTxnRef", event.getProviderTxnRef());
            LocalDateTime at = event.getOccurredAt() != null ? event.getOccurredAt() : LocalDateTime.now();
            ctx.setVariable("time", at.format(TIME_FMT));

            String html = templateEngine.process(template, ctx);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(mailFrom);
            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);

            saveLog(recipient, subject, type, event.getOrderId(), NotificationStatus.SENT, null);
            log.info("Sent {} email to {} for order #{}", type, recipient, event.getOrderId());
        } catch (Exception e) {
            saveLog(recipient, subject, type, event.getOrderId(), NotificationStatus.FAILED, e.getMessage());
            log.error("Failed to send {} email for order #{}: {}", type, event.getOrderId(), e.getMessage());
            throw new IllegalStateException("Send email failed: " + e.getMessage(), e);
        }
    }

    private String resolveEmail(NotificationEvent event) {
        // 1. Nếu có cấu hình email nhận test ghi đè (ví dụ MAIL_OVERRIDE_TO)
        if (overrideRecipient != null && !overrideRecipient.isBlank()) {
            return overrideRecipient.trim();
        }
        // 2. Ưu tiên email của khách hàng được truyền trong đơn hàng
        if (event.getCustomerEmail() != null && !event.getCustomerEmail().isBlank()) {
            return event.getCustomerEmail().trim();
        }
        // 3. Nếu userId có dạng email
        String userId = event.getUserId();
        if (userId != null && userId.contains("@")) {
            return userId.trim();
        }
        if (userId == null || userId.isBlank()) {
            return "unknown@" + defaultEmailDomain;
        }
        return userId + "@" + defaultEmailDomain;
    }

    private String formatMoney(Double amount) {
        if (amount == null) {
            return "0 ₫";
        }
        return NumberFormat.getInstance(new Locale("vi", "VN")).format(amount) + " ₫";
    }

    private void saveLog(String to, String subject, String type, Long refId, NotificationStatus status, String error) {
        logRepository.save(NotificationLog.builder()
                .recipientEmail(to)
                .subject(subject)
                .notificationType(type)
                .referenceId(refId)
                .status(status)
                .errorMessage(error != null && error.length() > 900 ? error.substring(0, 900) : error)
                .createdAt(LocalDateTime.now())
                .sentAt(status == NotificationStatus.SENT ? LocalDateTime.now() : null)
                .build());
    }
}
