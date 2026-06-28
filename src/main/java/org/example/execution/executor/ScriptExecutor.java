package org.example.execution.executor;

import org.example.domain.VulnerabilityScript;
import org.example.execution.plan.ExecutionPlan;
import org.example.execution.plan.PlanAnalysis;
import org.example.execution.simulation.SimulationReport;
import org.example.validation.ValidationResult;
import org.jspecify.annotations.NonNull;

import java.util.Collection;

public interface ScriptExecutor {

    @NonNull SimulationReport executeScripts(@NonNull Collection<VulnerabilityScript> scripts);

    @NonNull PlanAnalysis analyze(@NonNull ExecutionPlan plan);

    @NonNull ValidationResult validate(@NonNull ExecutionPlan plan);
}
