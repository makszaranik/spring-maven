package org.example.execution.executor;

import org.example.domain.VulnerabilityScript;
import org.example.execution.plan.ExecutionPlan;
import org.example.execution.plan.PlanAnalysis;
import org.example.execution.simulation.SimulationReport;
import org.example.validation.ValidationContext;
import org.example.validation.ValidationResult;

import java.util.Collection;

public interface ScriptExecutor {

    SimulationReport executeScripts(Collection<VulnerabilityScript> scripts);

    PlanAnalysis analyze(ExecutionPlan plan);

    ValidationResult validate(ExecutionPlan plan);
}
