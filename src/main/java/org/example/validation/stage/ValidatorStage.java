package org.example.validation.stage;

import org.example.validation.ValidationChain;
import org.example.validation.ValidationContext;

public interface ValidatorStage {

    default void executeValidationBeforePlan(ValidationContext context, ValidationChain chain) {}

    default void executeValidationAfterPlan(ValidationContext context, ValidationChain chain) {}

}
