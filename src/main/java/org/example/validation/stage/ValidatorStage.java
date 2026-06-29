package org.example.validation.stage;

import org.example.execution.plan.ExecutionPlan;
import org.example.validation.ValidationContext;
import org.example.validation.chain.ValidationChain;
import org.jspecify.annotations.NonNull;

public interface ValidatorStage {

    default void executeValidationBeforePlan(@NonNull ValidationContext context, @NonNull ValidationChain chain) {
        chain.doNext(chain, context);
    }

    default void executeValidationAfterPlan(@NonNull ValidationContext context, @NonNull ValidationChain chain, @NonNull ExecutionPlan plan) {
        chain.doNext(chain, context, plan);
    }

}
