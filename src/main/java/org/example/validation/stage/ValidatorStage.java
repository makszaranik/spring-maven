package org.example.validation.stage;

import org.example.execution.plan.ExecutionPlan;
import org.example.validation.ValidationContext;
import org.example.validation.chain.ValidationChain;

public interface ValidatorStage {

    default void executeValidationBeforePlan(ValidationContext context, ValidationChain chain) {}

    default void executeValidationAfterPlan(ValidationContext context, ValidationChain chain, ExecutionPlan plan) {}

}
