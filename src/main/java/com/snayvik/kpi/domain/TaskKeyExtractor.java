package com.snayvik.kpi.domain;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TaskKeyExtractor {

    private final Pattern taskKeyPattern;

    public TaskKeyExtractor(String regex) {
        this.taskKeyPattern = Pattern.compile(regex);
    }

    public Set<String> extractTaskKeys(Collection<String> candidates) {
        Set<String> taskKeys = new LinkedHashSet<>();
        if (candidates == null) {
            return taskKeys;
        }

        for (String candidate : candidates) {
            if (candidate == null || candidate.isBlank()) {
                continue;
            }
            Matcher matcher = taskKeyPattern.matcher(candidate);
            while (matcher.find()) {
                taskKeys.add(matcher.group(1));
            }
        }
        return taskKeys;
    }
}
