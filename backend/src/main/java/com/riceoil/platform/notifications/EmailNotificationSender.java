package com.riceoil.platform.notifications;

public interface EmailNotificationSender {
    void sendPasswordReset(String recipient, String resetUrl);

    void sendOrderConfirmation(String recipient, String orderNumber, String body);

    void sendOrderUpdate(String recipient, String subject, String body);
}
