package org.example.validation;

import lombok.RequiredArgsConstructor;
import org.example.validation.stage.ValidatorStage;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
@RequiredArgsConstructor
public class ValidationChain {

    private final List<ValidatorStage> validatorStages;
    private final ThreadLocal<Integer> currentPosition = ThreadLocal.withInitial(() -> 0);

    public void doNext(ValidationContext context) {
        if (context.isCanProceed() && currentPosition.get() < validatorStages.size()) {
            ValidatorStage nextStage = validatorStages.get(currentPosition.get());
            currentPosition.set(currentPosition.get() + 1);
            nextStage.executeValidationBeforePlan(context, this);
        } else {
            currentPosition.remove();
        }
    }

    public void start(ValidationContext context) {
        currentPosition.set(0);
        doNext(context);
    }

}
