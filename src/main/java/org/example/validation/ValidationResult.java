package org.example.validation;

import lombok.Builder;
import org.example.domain.VulnerabilityScript;
import org.example.execution.plan.ExecutionPlan;

import java.util.List;

@Builder
public record ValidationResult(
    List<String> warnings,
    List<String> errors,
    List<VulnerabilityScript> validScripts,
    ExecutionPlan executionPlan
) {}
