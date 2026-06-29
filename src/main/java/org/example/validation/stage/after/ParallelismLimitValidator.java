package org.example.validation.stage.after;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.config.PlannerConfig;
import org.example.execution.plan.ExecutionPlan;
import org.example.validation.ValidationContext;
import org.example.validation.chain.ValidationChain;
import org.example.validation.stage.ValidatorStage;
import org.jspecify.annotations.NonNull;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Order(8)
@Component
@RequiredArgsConstructor
public class ParallelismLimitValidator implements ValidatorStage {

    private final PlannerConfig plannerConfig;

    @Override
    public void executeValidationAfterPlan(@NonNull ValidationContext context, @NonNull ValidationChain chain, @NonNull ExecutionPlan plan) {
        int maxLimit = plannerConfig.maxParallelExecutions();

        for (int i = 0; i < plan.waves().size(); i++) {
            int currentWaveSize = plan.waves().get(i).scripts().size();
            if (currentWaveSize > maxLimit) {
                log.info("Wave {} exceeds the parallelism limit. Contains {} scripts, limit is {}.", i, currentWaveSize, maxLimit);
                context.addErrorLog(String.format("Wave %d exceeds the parallelism limit. Contains %d scripts, limit is %d.", i, currentWaveSize, maxLimit));
            }
        }

        log.info("ParallelismLimitValidator");
        chain.doNext(chain, context, plan);
    }
}