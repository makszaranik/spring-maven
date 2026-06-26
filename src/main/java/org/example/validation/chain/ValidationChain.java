package org.example.validation.chain;

import org.example.execution.plan.ExecutionPlan;
import org.example.validation.ValidationContext;

public interface ValidationChain {

    void doNextBefore(ValidationContext context);

    void startBeforeChain(ValidationContext context);

    void doNextAfter(ValidationContext context, ExecutionPlan plan);

    void startAfterChain(ValidationContext context, ExecutionPlan plan);

}
