package com.snayvik.kpi.policy;

public final class ViolationType {

    public static final String DONE_WITHOUT_MERGE = "DONE_WITHOUT_MERGE";
    public static final String MISSING_TASK_KEY = "MISSING_TASK_KEY";
    public static final String MULTIPLE_TASK_KEYS = "MULTIPLE_TASK_KEYS";
    public static final String DRIFT_AFTER_START = "DRIFT_AFTER_START";
    public static final String UNTRACKED_WORK = "UNTRACKED_WORK";
    public static final String NO_TIME_ON_COMPLETED_TASK = "NO_TIME_ON_COMPLETED_TASK";
    public static final String UNJUSTIFIED_TIME = "UNJUSTIFIED_TIME";

    private ViolationType() {
    }
}
