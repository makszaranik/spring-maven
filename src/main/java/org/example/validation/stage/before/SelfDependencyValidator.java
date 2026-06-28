package org.example.validation.stage.before;

import org.example.domain.VulnerabilityScript;
import org.example.validation.ValidationContext;
import org.example.validation.chain.ValidationChain;
import org.example.validation.stage.ValidatorStage;
import org.jspecify.annotations.NonNull;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Order(3)
@Component
public class SelfDependencyValidator implements ValidatorStage {

    @Override
    public void executeValidationBeforePlan(@NonNull ValidationContext context, @NonNull ValidationChain chain) {
        for (VulnerabilityScript script : context.scripts()) {
            List<Integer> deps = script.getDependencies();
            if (deps != null && deps.contains(script.getScriptId())) {
                context.addWarningLog(String.format("Script %s depends on itself", script.getScriptId()));
            }
        }
        chain.doNext(chain, context);
    }
}
