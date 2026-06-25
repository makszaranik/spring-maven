package org.example.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "planner")
public record PlannerConfig(
    int maxParallelExecutions
) {}
