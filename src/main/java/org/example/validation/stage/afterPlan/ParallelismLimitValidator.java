package org.example.validation.stage.afterPlan;

import lombok.RequiredArgsConstructor;
import org.example.config.PlannerConfig;
import org.example.execution.plan.ExecutionPlan;
import org.example.validation.ValidationContext;
import org.example.validation.chain.ValidationChain;
import org.example.validation.stage.ValidatorStage;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ParallelismLimitValidator implements ValidatorStage {

    private final PlannerConfig plannerConfig;

    @Override
    public void executeValidationAfterPlan(ValidationContext context, ValidationChain chain, ExecutionPlan plan) {

        int maxLimit = plannerConfig.maxParallelExecutions();

        for (int i = 0; i < plan.waves().size(); i++) {
            int currentWaveSize = plan.waves().get(i).scripts().size();

            if (currentWaveSize > maxLimit) {
                context.addErrorLog(String.format(
                    "Wave %d exceeds the parallelism limit. Contains %d scripts, limit is %d.",
                    i, currentWaveSize, maxLimit
                ));
            }
        }
        chain.doNextAfter(context, plan);
    }
}