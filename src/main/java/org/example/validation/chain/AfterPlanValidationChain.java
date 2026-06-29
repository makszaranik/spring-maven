package org.example.validation.chain;

import lombok.RequiredArgsConstructor;
import org.example.execution.plan.ExecutionPlan;
import org.example.validation.ValidationContext;
import org.example.validation.stage.ValidatorStage;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AfterPlanValidationChain implements ValidationChain {

    private final List<ValidatorStage> validatorStages;
    private final ThreadLocal<Integer> currentPositionAfter = ThreadLocal.withInitial(() -> 0);

    public void startChain(@NonNull ValidationContext context, @NonNull ExecutionPlan plan) {
        currentPositionAfter.set(0);
        this.doNext(this, context, plan);
    }

    @Override
    public void doNext(@NonNull ValidationChain chain, @NonNull ValidationContext context, @NonNull ExecutionPlan plan) {
        if (currentPositionAfter.get() < validatorStages.size()) {
            ValidatorStage nextStage = validatorStages.get(currentPositionAfter.get());
            currentPositionAfter.set(currentPositionAfter.get() + 1);
            nextStage.executeValidationAfterPlan(context, this, plan);
        }
    }
}
