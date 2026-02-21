package com.snayvik.kpi.sync;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.sync")
public class SyncProperties {

    private boolean enabled = false;
    private boolean initialRunOnStartup = false;
    private String reconciliationCron = "0 30 2 * * *";
    private int githubLookbackDays = 90;
    private List<String> githubRepositories = new ArrayList<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isInitialRunOnStartup() {
        return initialRunOnStartup;
    }

    public void setInitialRunOnStartup(boolean initialRunOnStartup) {
        this.initialRunOnStartup = initialRunOnStartup;
    }

    public String getReconciliationCron() {
        return reconciliationCron;
    }

    public void setReconciliationCron(String reconciliationCron) {
        this.reconciliationCron = reconciliationCron;
    }

    public int getGithubLookbackDays() {
        return githubLookbackDays;
    }

    public void setGithubLookbackDays(int githubLookbackDays) {
        this.githubLookbackDays = githubLookbackDays;
    }

    public List<String> getGithubRepositories() {
        return githubRepositories;
    }

    public void setGithubRepositories(List<String> githubRepositories) {
        this.githubRepositories = githubRepositories;
    }
}
