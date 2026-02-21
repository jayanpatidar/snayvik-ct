package com.snayvik.kpi.ingress.audit;

import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookEventRepository extends JpaRepository<WebhookEvent, Long> {

    boolean existsBySourceAndDedupeKey(String source, String dedupeKey);

    long countByStatus(String status);

    long deleteByReceivedAtBefore(Instant cutoff);
}
