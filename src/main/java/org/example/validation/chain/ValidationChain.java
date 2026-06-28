package org.example.validation.chain;

import org.example.execution.plan.ExecutionPlan;
import org.example.validation.ValidationContext;
import org.jspecify.annotations.NonNull;

public interface ValidationChain {

    default void doNext(@NonNull ValidationChain chain, @NonNull ValidationContext context, @NonNull ExecutionPlan plan) {
        chain.doNext(chain, context, plan);
    }

    default void doNext(@NonNull ValidationChain chain, @NonNull ValidationContext context) {
        chain.doNext(chain, context);
    }

}