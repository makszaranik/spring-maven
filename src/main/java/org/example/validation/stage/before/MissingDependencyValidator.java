package org.example.validation.stage.before;

import org.example.domain.VulnerabilityScript;
import org.example.validation.ValidationContext;
import org.example.validation.chain.ValidationChain;
import org.example.validation.stage.ValidatorStage;
import org.jspecify.annotations.NonNull;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Order(1)
@Component
public class MissingDependencyValidator implements ValidatorStage {

    @Override
    public void executeValidationBeforePlan(@NonNull ValidationContext context, @NonNull ValidationChain chain) {

        Set<Integer> existingScriptIds = context.scripts().stream()
            .map(VulnerabilityScript::getScriptId)
            .collect(Collectors.toSet());

        for (VulnerabilityScript script : context.scripts()) {
            List<Integer> dependencies = script.getDependencies();
            for (Integer depId : dependencies) {
                if (!existingScriptIds.contains(depId)) {
                    context.addErrorLog(String.format("Script %s contains missing dependency: %s", script.getScriptId(), depId));
                }
            }
        }

        chain.doNext(chain, context);
    }
}