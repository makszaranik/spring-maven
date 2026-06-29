package org.example.validation.chain;

import lombok.RequiredArgsConstructor;
import org.example.validation.ValidationContext;
import org.example.validation.stage.ValidatorStage;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class BeforePlanValidationChain implements ValidationChain {

    private final List<ValidatorStage> validatorStages;
    private final ThreadLocal<Integer> currentPositionBefore = ThreadLocal.withInitial(() -> 0);

    public void startChain(@NonNull ValidationContext context) {
        currentPositionBefore.set(0);
        this.doNext(this, context);
    }

    @Override
    public void doNext(@NonNull ValidationChain chain, @NonNull ValidationContext context) {
        if (currentPositionBefore.get() < validatorStages.size()) {
            ValidatorStage nextStage = validatorStages.get(currentPositionBefore.get());
            currentPositionBefore.set(currentPositionBefore.get() + 1);
            nextStage.executeValidationBeforePlan(context, this);
        }
    }
}
