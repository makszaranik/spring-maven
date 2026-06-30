package org.example.execution.executor;

import org.example.domain.VulnerabilityScript;
import org.example.execution.plan.ExecutionPlan;
import org.example.execution.plan.PlanAnalysis;
import org.example.execution.simulation.SimulationReport;
import org.example.validation.ValidationContext;
import org.example.validation.ValidationResult;
import org.jspecify.annotations.NonNull;

import java.util.Collection;

public interface ScriptExecutor {

    @NonNull SimulationReport executeScripts(@NonNull ExecutionPlan executionPlan);

    @NonNull ValidationResult addAndValidateScript(@NonNull ExecutionPlan plan, @NonNull VulnerabilityScript newScript, @NonNull ValidationContext context);

    @NonNull PlanAnalysis analyze(@NonNull Collection<@NonNull VulnerabilityScript> scripts, @NonNull ExecutionPlan plan);

    @NonNull ValidationResult validate(@NonNull Collection<@NonNull VulnerabilityScript> scripts, @NonNull ValidationContext context);
}
