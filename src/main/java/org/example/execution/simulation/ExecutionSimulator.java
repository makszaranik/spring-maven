package org.example.execution.simulation;

import org.example.domain.VulnerabilityScript;
import org.example.execution.plan.ExecutionPlan;
import org.jspecify.annotations.NonNull;

import java.util.Collection;

public interface ExecutionSimulator {
    @NonNull SimulationReport simulate(@NonNull ExecutionPlan plan, @NonNull Collection<VulnerabilityScript> scripts);
}
