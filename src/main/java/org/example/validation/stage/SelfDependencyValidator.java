package org.example.validation.stage;

import org.example.domain.VulnerabilityScript;
import org.example.validation.ValidationChain;
import org.example.validation.ValidationContext;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SelfDependencyValidator implements ValidatorStage {

    @Override
    public void executeValidation(ValidationContext context, ValidationChain chain) {
        for (VulnerabilityScript script : context.getScripts()) {
            List<Integer> deps = script.getDependencies();
            if (deps != null && deps.contains(script.getScriptId())) {
                context.addWarningLog(String.format("Script %s depends on itself", script.getScriptId()));
            }
        }
        chain.doNext(context);
    }
}
