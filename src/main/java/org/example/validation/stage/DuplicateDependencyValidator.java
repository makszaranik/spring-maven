package org.example.validation.stage;

import org.example.domain.VulnerabilityScript;
import org.example.validation.ValidationChain;
import org.example.validation.ValidationContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class DuplicateDependencyValidator implements ValidatorStage {

    @Override
    public void executeValidation(ValidationContext context, ValidationChain chain) {
        for (VulnerabilityScript script : context.getScripts()) {
            List<Integer> deps = script.getDependencies();
            if (deps != null && !deps.isEmpty()) {
                Set<Integer> uniqueDeps = new HashSet<>(deps);
                if (uniqueDeps.size() < deps.size()) {
                    context.addWarningLog(String.format("Script %s contains duplicated dependencies", script.getScriptId()));
                    script.setDependencies(new ArrayList<>(uniqueDeps));
                }
            }
        }
        chain.doNext(context);
    }
}
