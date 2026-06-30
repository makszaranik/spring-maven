package org.example.execution.plan;

import lombok.Builder;

import java.util.List;

@Builder
public record PlanAnalysis(
    int totalScripts,
    int totalWaves,
    int maxParallelismAchieved,
    double averageParallelism,
    double efficiency,
    int criticalPathLength,
    long estimatedExecutionTime
) {}
