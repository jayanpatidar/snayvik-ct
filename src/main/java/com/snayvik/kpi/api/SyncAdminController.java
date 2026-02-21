package com.snayvik.kpi.api;

import com.snayvik.kpi.sync.InitialFullSyncService;
import com.snayvik.kpi.sync.SyncRunReport;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/kpi/admin/sync")
public class SyncAdminController {

    private final InitialFullSyncService initialFullSyncService;

    public SyncAdminController(InitialFullSyncService initialFullSyncService) {
        this.initialFullSyncService = initialFullSyncService;
    }

    @PostMapping("/full")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public SyncRunReport runFullSync() {
        return initialFullSyncService.runInitialFullSync();
    }

    @PostMapping("/reconcile")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public SyncRunReport runReconciliation() {
        return initialFullSyncService.runReconciliation();
    }
}
