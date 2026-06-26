package org.example.validation.stage.afterPlan;

import lombok.RequiredArgsConstructor;
import org.example.config.PlannerConfig;
import org.example.domain.VulnerabilityScript;
import org.example.execution.plan.ExecutionPlan;
import org.example.validation.ValidationContext;
import org.example.validation.chain.ValidationChain;
import org.example.validation.stage.ValidatorStage;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class MinimalWavesValidator implements ValidatorStage {

    private final PlannerConfig plannerConfig;

    @Override
    public void executeValidationAfterPlan(ValidationContext context, ValidationChain chain, ExecutionPlan plan) {

        List<ExecutionPlan.ExecutionWave> waves = plan.waves();
        int maxLimit = plannerConfig.maxParallelExecutions();

        Set<Integer> completedBeforePrevWave = new HashSet<>();
        Set<Integer> completedBeforeCurrentWave = new HashSet<>();

        for (int i = 0; i < waves.size(); i++) {
            ExecutionPlan.ExecutionWave currentWave = waves.get(i);

            if (i > 0) {
                ExecutionPlan.ExecutionWave previousWave = waves.get(i - 1);
                if (previousWave.scripts().size() < maxLimit) {
                    for (VulnerabilityScript script : currentWave.scripts()) {
                        List<Integer> deps = script.getDependencies();
                        boolean canBeMovedBack = (deps == null || deps.isEmpty()) || completedBeforePrevWave.containsAll(deps);

                        if (canBeMovedBack) {
                            context.addErrorLog(String.format(
                                "Plan is not minimal. Script %d in wave %d could have been placed in wave %d.",
                                script.getScriptId(), i, i - 1
                            ));
                        }
                    }
                }
            }

            completedBeforePrevWave.clear();
            completedBeforePrevWave.addAll(completedBeforeCurrentWave);

            for (VulnerabilityScript s : currentWave.scripts()) {
                completedBeforeCurrentWave.add(s.getScriptId());
            }
        }
        chain.doNextAfter(context, plan);
    }
}