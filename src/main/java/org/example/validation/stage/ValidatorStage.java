package org.example.validation.stage;

import org.example.validation.ValidationChain;
import org.example.validation.ValidationContext;

public interface ValidatorStage {
    void executeValidation(ValidationContext context, ValidationChain chain);
}
