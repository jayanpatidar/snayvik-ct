package com.snayvik.kpi.sync;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "app.sync",
        name = {"enabled", "initial-run-on-startup"},
        havingValue = "true")
public class InitialFullSyncStartupRunner implements ApplicationRunner {

    private final InitialFullSyncService initialFullSyncService;

    public InitialFullSyncStartupRunner(InitialFullSyncService initialFullSyncService) {
        this.initialFullSyncService = initialFullSyncService;
    }

    @Override
    public void run(ApplicationArguments args) {
        initialFullSyncService.runInitialFullSync();
    }
}
