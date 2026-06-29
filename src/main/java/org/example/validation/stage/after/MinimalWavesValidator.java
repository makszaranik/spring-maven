package org.example.validation.stage.after;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.config.PlannerConfig;
import org.example.domain.VulnerabilityScript;
import org.example.execution.plan.ExecutionPlan;
import org.example.validation.ValidationContext;
import org.example.validation.chain.ValidationChain;
import org.example.validation.stage.ValidatorStage;
import org.jspecify.annotations.NonNull;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Order(7)
@Component
@RequiredArgsConstructor
public class MinimalWavesValidator implements ValidatorStage {

    private final PlannerConfig plannerConfig;

    @Override
    public void executeValidationAfterPlan(@NonNull ValidationContext context, @NonNull ValidationChain chain, @NonNull ExecutionPlan plan) {
        List<ExecutionPlan.ExecutionWave> waves = plan.waves();
        int maxLimit = plannerConfig.maxParallelExecutions();

        Map<Integer, Integer> scriptWaveMap = new HashMap<>();
        for (int i = 0; i < waves.size(); i++) {
            for (VulnerabilityScript s : waves.get(i).scripts()) {
                scriptWaveMap.put(s.getScriptId(), i);
            }
        }

        for (int i = 1; i < waves.size(); i++) {
            ExecutionPlan.ExecutionWave currentWave = waves.get(i);

            for (VulnerabilityScript script : currentWave.scripts()) {
                List<Integer> deps = script.getDependencies() == null ? Collections.emptyList() : script.getDependencies();

                for (int targetWaveIndex = 0; targetWaveIndex < i; targetWaveIndex++) {
                    if (waves.get(targetWaveIndex).scripts().size() < maxLimit) {

                        boolean allDepsCompleted = true;
                        for (Integer depId : deps) {
                            Integer depWave = scriptWaveMap.get(depId);
                            if (depWave == null || depWave >= targetWaveIndex) {
                                allDepsCompleted = false;
                                break;
                            }
                        }

                        if (allDepsCompleted) {
                            log.error("Plan is not minimal. Script {} in wave {} could have been placed earlier in wave {}.", script.getScriptId(), i, targetWaveIndex);
                            context.addErrorLog(String.format("Plan is not minimal. Script %d in wave %d could have been placed earlier in wave %d.", script.getScriptId(), i, targetWaveIndex));
                            break;
                        }
                    }
                }
            }
        }

        log.info("MinimalWavesValidator finished");
        chain.doNext(chain, context, plan);
    }
}