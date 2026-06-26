package org.example.validation.stage.afterPlan;

import org.example.execution.plan.ExecutionPlan;
import org.example.validation.ValidationContext;
import org.example.validation.chain.ValidationChain;
import org.example.validation.stage.ValidatorStage;
import org.springframework.stereotype.Component;

@Component
public class ScriptAppearsOnceValidator implements ValidatorStage {

    @Override
    public void executeValidationAfterPlan(ValidationContext context, ValidationChain chain, ExecutionPlan plan) {
        chain.doNextAfter(context, plan);
    }
}
