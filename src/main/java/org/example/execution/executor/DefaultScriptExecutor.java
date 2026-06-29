package org.example.execution.executor;

import lombok.RequiredArgsConstructor;
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
import org.example.validation.chain.ValidationChain;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DefaultScriptExecutor implements ScriptExecutor {

    private final ExecutionPlanner executionPlanner;
    private final ExecutionSimulator executionSimulator;
    private final BeforePlanValidationChain beforePlanValidationChain;
    private final AfterPlanValidationChain afterPlanValidationChain;
    private final PlannerConfig plannerConfig;

    @Override
    public @NonNull SimulationReport executeScripts(@NonNull Collection<VulnerabilityScript> scripts, @NonNull ExecutionPlan executionPlan) {
        return executionSimulator.simulate(executionPlan, scripts);
    }

    @Override
    public @NonNull PlanAnalysis analyze(@NonNull ExecutionPlan plan) {
        List<VulnerabilityScript> allScripts = plan.waves().stream()
            .flatMap(w -> w.scripts().stream())
            .toList();

        DependencyGraph graph = DependencyGraph.buildGraph(allScripts);
        List<List<Integer>> cycles = DependencyGraphUtils.findAllCycles(graph);
        DependencyGraphUtils.CriticalPath criticalPath = DependencyGraphUtils.calculateCriticalPath(graph);

        int totalScripts = allScripts.size();
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
            .cycles(cycles)
            .build();

    }

    @Override
    public @NonNull ValidationResult validate(@NonNull ExecutionPlan plan) {
        List<VulnerabilityScript> scripts = plan.waves().stream()
            .flatMap(executionWave -> executionWave.scripts().stream())
            .toList();

        ValidationContext context = ValidationContext.builder()
            .scripts(scripts)
            .warnings(new ArrayList<>())
            .errors(new ArrayList<>())
            .build();

        beforePlanValidationChain.startChain(context);
        afterPlanValidationChain.startChain(context, plan);

        return ValidationResult.builder()
            .warnings(context.warnings())
            .errors(context.errors())
            .build();
    }

    private double calculateEfficiency(int totalWaves, int maxLimit, int totalScripts) {
        return (totalWaves == 0 || maxLimit == 0) ? 0 : (double) totalScripts / (totalWaves * maxLimit);
    }

    private double calculateAvgParallelism(int totalWaves, int totalScripts) {
        return (totalWaves == 0 ? 0 : (double) totalScripts / totalWaves);
    }

}
