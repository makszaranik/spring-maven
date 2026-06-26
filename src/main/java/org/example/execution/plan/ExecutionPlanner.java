package org.example.execution.plan;

import org.example.config.PlannerConfig;
import org.example.domain.VulnerabilityScript;
import org.example.validation.ValidationResult;

import java.util.Collection;

public interface ExecutionPlanner {

    ExecutionPlan createPlan(Collection<VulnerabilityScript> scripts);
    //ValidationResult validate(ExecutionPlan plan);

    /*
    PlanAnalysis analyze(
        ExecutionPlan plan
    );

     */

    void addScript(ExecutionPlan plan, VulnerabilityScript script);
}
