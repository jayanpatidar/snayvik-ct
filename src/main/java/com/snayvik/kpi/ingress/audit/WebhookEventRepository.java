package com.snayvik.kpi.ingress.audit;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookEventRepository extends JpaRepository<WebhookEvent, Long> {

    boolean existsBySourceAndDedupeKey(String source, String dedupeKey);
}
