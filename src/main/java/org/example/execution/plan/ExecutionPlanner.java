package org.example.execution.plan;

import org.example.domain.VulnerabilityScript;
import org.jspecify.annotations.NonNull;

import java.util.Collection;

public interface ExecutionPlanner {

    @NonNull ExecutionPlan createPlan(@NonNull Collection<@NonNull VulnerabilityScript> scripts);

    void addScript(@NonNull ExecutionPlan plan, @NonNull VulnerabilityScript script);
}
