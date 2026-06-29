package org.example.execution.simulation;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.example.config.PlannerConfig;
import org.example.config.SimulationConfig;
import org.example.domain.VulnerabilityScript;
import org.example.execution.plan.ExecutionPlan;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.*;

@Component
@RequiredArgsConstructor
public class ParallelExecutionSimulator implements ExecutionSimulator {

    private final SimulationConfig simulationConfig;
    private final PlannerConfig plannerConfig;

    @Override
    @SneakyThrows
    public @NonNull SimulationReport simulate(@NonNull ExecutionPlan plan) {
        List<Integer> failedScripts = Collections.synchronizedList(new ArrayList<>());
        List<Integer> successfulCompletedScripts = Collections.synchronizedList(new ArrayList<>());
        Map<Integer, Integer> retryStatistics = new ConcurrentHashMap<>();
        int totalExecutionTime = 0;

        try (ExecutorService executor = Executors.newFixedThreadPool(plannerConfig.maxParallelExecutions())) {

            for (ExecutionPlan.ExecutionWave wave : plan.waves()) {
                List<VulnerabilityScript> readyToExecute = wave.scripts().stream()
                    .filter(script -> !hasFailedDependencies(script, failedScripts))
                    .toList();

                List<Callable<ScriptResult>> tasksToExecute = readyToExecute.stream()
                    .map(script -> (Callable<ScriptResult>) () -> executeScript(script))
                    .toList();

                List<Future<ScriptResult>> futureTasks = executor.invokeAll(tasksToExecute);
                int maxWaveTime = 0;

                for (Future<ScriptResult> resultFuture : futureTasks) {
                    ScriptResult result = resultFuture.get();

                    if (result.success()) {
                        successfulCompletedScripts.add(result.scriptId());
                    } else {
                        failedScripts.add(result.scriptId());
                    }

                    retryStatistics.put(result.scriptId(), result.retries());
                    maxWaveTime = Math.max(maxWaveTime, result.executionTime());
                }
                totalExecutionTime += maxWaveTime;
            }
        }

        return SimulationReport.builder()
            .successfulCompletedScripts(successfulCompletedScripts)
            .failedScripts(failedScripts)
            .retryStatistics(retryStatistics)
            .totalExecutionTime(totalExecutionTime)
            .build();
    }


    private boolean isExecutedSuccessfully() {
        return ThreadLocalRandom.current().nextDouble() >= simulationConfig.failureProbability();
    }

    private boolean hasFailedDependencies(VulnerabilityScript script, List<Integer> failedOrAbortedIds) {
        if (script.getDependencies() == null) return false;
        return script.getDependencies().stream().anyMatch(failedOrAbortedIds::contains);
    }

    private int scriptExecutionTime(VulnerabilityScript script, int attemptsNumber) {
        return script.getEstimatedDurationSeconds() * attemptsNumber;
    }

    private ScriptResult executeScript(VulnerabilityScript script) {
        int currentExecutionAttempts = 0;
        boolean isCompletedSuccess = false;

        while (currentExecutionAttempts < simulationConfig.maxRetries() && !isCompletedSuccess) {
            if (isExecutedSuccessfully()) {
                isCompletedSuccess = true;
            }
            currentExecutionAttempts++;
        }

        return ScriptResult.builder()
            .scriptId(script.getScriptId())
            .success(isCompletedSuccess)
            .retries(isCompletedSuccess ? currentExecutionAttempts - 1 : currentExecutionAttempts)
            .executionTime(scriptExecutionTime(script, currentExecutionAttempts))
            .build();
    }

    @Builder
    private record ScriptResult(
        int scriptId,
        boolean success,
        int retries,
        int executionTime
    ) {
    }
}
