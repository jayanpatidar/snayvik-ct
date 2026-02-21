package com.snayvik.kpi.policy;

public final class ViolationType {

    public static final String DONE_WITHOUT_MERGE = "DONE_WITHOUT_MERGE";
    public static final String MISSING_TASK_KEY = "MISSING_TASK_KEY";
    public static final String MULTIPLE_TASK_KEYS = "MULTIPLE_TASK_KEYS";
    public static final String DRIFT_AFTER_START = "DRIFT_AFTER_START";

    private ViolationType() {
    }
}
