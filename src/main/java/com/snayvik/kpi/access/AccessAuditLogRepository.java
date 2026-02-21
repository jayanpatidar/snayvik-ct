package com.snayvik.kpi.access;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccessAuditLogRepository extends JpaRepository<AccessAuditLog, Long> {

    List<AccessAuditLog> findTop100ByOrderByTimestampDesc();
}
