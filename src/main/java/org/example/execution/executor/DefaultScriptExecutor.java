package org.example.execution.executor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.config.PlannerConfig;
import org.example.domain.VulnerabilityScript;
import org.example.execution.plan.ExecutionPlan;
import org.example.execution.plan.ExecutionPlanner;
import org.example.execution.plan.PlanAnalysis;
import org.example.execution.simulation.ExecutionSimulator;
import org.example.execution.simulation.SimulationReport;
import org.example.graph.DependencyGraph;
import org.example.graph.DependencyGraphUtils;
import org.example.validation.ValidationContext;
import org.example.validation.ValidationResult;
import org.example.validation.chain.AfterPlanValidationChain;
import org.example.validation.chain.BeforePlanValidationChain;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultScriptExecutor implements ScriptExecutor {

    private final ExecutionPlanner executionPlanner;
    private final ExecutionSimulator executionSimulator;
    private final BeforePlanValidationChain beforePlanValidationChain;
    private final AfterPlanValidationChain afterPlanValidationChain;
    private final PlannerConfig plannerConfig;

    @Override
    public @NonNull SimulationReport executeScripts(@NonNull ExecutionPlan executionPlan) {
        return executionSimulator.simulate(executionPlan);
    }

    @Override
    public @NonNull ValidationResult addAndValidateScript(@NonNull ExecutionPlan plan, @NonNull VulnerabilityScript newScript, @NonNull ValidationContext context) {
        executionPlanner.addScript(plan, newScript);
        List<VulnerabilityScript> scripts = extractAllScriptsFromPlan(plan);

        ValidationContext newContext = ValidationContext.builder()
            .validScripts(new ArrayList<>(scripts))
            .warnings(new ArrayList<>(context.warnings()))
            .errors(new ArrayList<>(context.errors()))
            .build();

        beforePlanValidationChain.startChain(newContext);
        afterPlanValidationChain.startChain(newContext, plan);

        log.info("add a new script and validation executed, valid scripts: {}", context.validScripts());

        return ValidationResult.builder()
            .errors(newContext.errors())
            .warnings(newContext.warnings())
            .validExecutionPlan(plan)
            .validScripts(newContext.validScripts())
            .build();
    }


    @Override
    public @NonNull PlanAnalysis analyze(@NonNull Collection<@NonNull VulnerabilityScript> scripts, @NonNull ExecutionPlan plan) {
        DependencyGraph graph = DependencyGraph.buildGraph(scripts);
        DependencyGraphUtils.CriticalPath criticalPath = DependencyGraphUtils.calculateCriticalPath(graph);

        int totalScripts = scripts.size();
        int totalWaves = plan.waves().size();

        double avgParallelism = calculateAvgParallelism(totalWaves, totalScripts);
        int maxParallelism = plan.waves().stream().mapToInt(w -> w.scripts().size()).max().orElse(0);
        int maxLimit = plannerConfig.maxParallelExecutions();
        double efficiency = calculateEfficiency(totalWaves, maxLimit, totalScripts);

        return PlanAnalysis.builder()
            .totalScripts(totalScripts)
            .totalWaves(totalWaves)
            .maxParallelismAchieved(maxParallelism)
            .averageParallelism(avgParallelism)
            .efficiency(efficiency)
            .criticalPathLength(criticalPath.duration())
            .estimatedExecutionTime(criticalPath.duration())
            .build();

    }

    @Override
    public @NonNull ValidationResult validate(@NonNull Collection<@NonNull VulnerabilityScript> scripts, @NonNull ValidationContext context) {
        beforePlanValidationChain.startChain(context);
        ExecutionPlan executionPlan = executionPlanner.createPlan(context.validScripts());
        afterPlanValidationChain.startChain(context, executionPlan);

        log.info("validation executed, valid scripts: {}", context.validScripts());

        return ValidationResult.builder()
            .warnings(context.warnings())
            .errors(context.errors())
            .validScripts(context.validScripts())
            .validExecutionPlan(executionPlan)
            .build();
    }

    private double calculateEfficiency(int totalWaves, int maxLimit, int totalScripts) {
        return (totalWaves == 0 || maxLimit == 0) ? 0 : (double) totalScripts / (totalWaves * maxLimit);
    }

    private double calculateAvgParallelism(int totalWaves, int totalScripts) {
        return (totalWaves == 0 ? 0 : (double) totalScripts / totalWaves);
    }

    private List<VulnerabilityScript> extractAllScriptsFromPlan(ExecutionPlan plan) {
        return plan.waves().stream()
            .flatMap(wave -> wave.scripts().stream())
            .toList();
    }
}
