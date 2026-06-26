package org.example.validation.chain;

import lombok.RequiredArgsConstructor;
import org.example.execution.plan.ExecutionPlan;
import org.example.validation.ValidationContext;
import org.example.validation.stage.ValidatorStage;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DefaultValidationChain implements ValidationChain {

    private final List<ValidatorStage> validatorStages;
    private int currentPositionBefore = 0;
    private int currentPositionAfter = 0;

    @Override
    public void doNextBefore(ValidationContext context) {
        if (context.isCanProceed() && currentPositionBefore < validatorStages.size()) {
            ValidatorStage nextStage = validatorStages.get(currentPositionBefore++);
            nextStage.executeValidationBeforePlan(context, this);
        }
    }

    @Override
    public void doNextAfter(ValidationContext context, ExecutionPlan plan) {
        if (context.isCanProceed() && currentPositionAfter < validatorStages.size()) {
            ValidatorStage nextStage = validatorStages.get(currentPositionAfter++);
            nextStage.executeValidationAfterPlan(context, this, plan);
        }
    }

    public void startBeforeChain(ValidationContext context) {
        ValidationChain chain = new DefaultValidationChain(validatorStages);
        chain.doNextBefore(context);
    }

    public void startAfterChain(ValidationContext context, ExecutionPlan plan) {
        ValidationChain chain = new DefaultValidationChain(validatorStages);
        chain.doNextAfter(context, plan);
    }
}