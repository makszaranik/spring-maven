package org.example.execution.simulation;

import lombok.Builder;

import java.util.List;
import java.util.Map;

@Builder
public record SimulationReport(
    List<Integer> successfulCompletedScripts,
    List<Integer> failedScripts,
    Map<Integer, Integer> retryStatistics,
    int totalExecutionTime
) {}
