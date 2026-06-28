package org.example.validation.stage.before;

import org.example.domain.VulnerabilityScript;
import org.example.validation.ValidationContext;
import org.example.validation.chain.ValidationChain;
import org.example.validation.stage.ValidatorStage;
import org.jspecify.annotations.NonNull;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Order(2)
@Component
public class DuplicateDependencyValidator implements ValidatorStage {

    @Override
    public void executeValidationBeforePlan(@NonNull ValidationContext context, @NonNull ValidationChain chain) {
        for (VulnerabilityScript script : context.scripts()) {
            List<Integer> deps = script.getDependencies();
            if (deps != null && !deps.isEmpty()) {
                Set<Integer> uniqueDeps = new HashSet<>(deps);
                if (uniqueDeps.size() < deps.size()) {
                    context.addWarningLog(String.format("Script %s contains duplicated dependencies", script.getScriptId()));
                    script.setDependencies(new ArrayList<>(uniqueDeps));
                }
            }
        }
        chain.doNext(chain, context);
    }
}
