package org.example.execution.plan;

import lombok.Builder;
import org.example.domain.VulnerabilityScript;

import java.util.List;

@Builder
public record ExecutionPlan(
    List<ExecutionWave> waves
) {
    public record ExecutionWave(
        List<VulnerabilityScript> scripts
    ) {}
}
