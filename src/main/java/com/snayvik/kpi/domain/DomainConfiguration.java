package com.snayvik.kpi.domain;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainConfiguration {

    @Bean
    public TaskKeyExtractor taskKeyExtractor(@Value("${app.task-key-regex}") String taskKeyRegex) {
        return new TaskKeyExtractor(taskKeyRegex);
    }
}
