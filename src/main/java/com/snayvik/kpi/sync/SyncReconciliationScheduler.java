package com.snayvik.kpi.sync;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SyncReconciliationScheduler {

    private final InitialFullSyncService initialFullSyncService;

    public SyncReconciliationScheduler(InitialFullSyncService initialFullSyncService) {
        this.initialFullSyncService = initialFullSyncService;
    }

    @Scheduled(cron = "${app.sync.reconciliation-cron:0 30 2 * * *}")
    public void runNightlyReconciliation() {
        initialFullSyncService.runReconciliation();
    }
}
