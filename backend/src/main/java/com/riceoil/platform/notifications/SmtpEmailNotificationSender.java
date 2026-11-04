package com.riceoil.platform.notifications;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class SmtpEmailNotificationSender implements EmailNotificationSender {

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public SmtpEmailNotificationSender(JavaMailSender mailSender,
                                       @Value("${app.mail.from}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    @Override
    public void sendPasswordReset(String recipient, String resetUrl) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(recipient);
        message.setSubject("Reset your Farmora password");
        message.setText("Use this link to reset your Farmora password. It expires in 30 minutes:\n\n"
                + resetUrl + "\n\nIf you did not request this, you can ignore this email.");
        mailSender.send(message);
    }

    @Override
    public void sendOrderUpdate(String recipient, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(recipient);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }

    @Override
    public void sendOrderConfirmation(String recipient, String orderNumber, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(recipient);
        message.setSubject("Your Farmora order " + orderNumber + " is confirmed");
        message.setText(body);
        mailSender.send(message);
    }
}
