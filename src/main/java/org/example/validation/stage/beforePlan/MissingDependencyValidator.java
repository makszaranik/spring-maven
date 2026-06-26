package org.example.validation.stage.beforePlan;

import org.example.domain.VulnerabilityScript;
import org.example.validation.ValidationContext;
import org.example.validation.chain.ValidationChain;
import org.example.validation.stage.ValidatorStage;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class MissingDependencyValidator implements ValidatorStage {

    @Override
    public void executeValidationBeforePlan(ValidationContext context, ValidationChain chain) {

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

        chain.doNextBefore(context);
    }
}