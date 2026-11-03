package com.riceoil.platform.payments.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentWebhookEventRepository extends JpaRepository<PaymentWebhookEventEntity, Long> {

    Optional<PaymentWebhookEventEntity> findByEventId(String eventId);
}
