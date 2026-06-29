package org.example.execution.simulation;

import org.example.execution.plan.ExecutionPlan;
import org.jspecify.annotations.NonNull;

public interface ExecutionSimulator {
    @NonNull SimulationReport simulate(@NonNull ExecutionPlan plan);
}
