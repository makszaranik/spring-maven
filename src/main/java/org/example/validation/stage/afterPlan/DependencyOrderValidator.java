package org.example.validation.stage.afterPlan;

import org.example.domain.VulnerabilityScript;
import org.example.execution.plan.ExecutionPlan;
import org.example.validation.ValidationContext;
import org.example.validation.chain.ValidationChain;
import org.example.validation.stage.ValidatorStage;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class DependencyOrderValidator implements ValidatorStage {

    @Override
    public void executeValidationAfterPlan(ValidationContext context, ValidationChain chain, ExecutionPlan plan) {

        Set<Integer> completedScripts = new HashSet<>();

        for (int i = 0; i < plan.waves().size(); i++) {
            ExecutionPlan.ExecutionWave wave = plan.waves().get(i);

            for (VulnerabilityScript script : wave.scripts()) {
                List<Integer> dependencies = script.getDependencies();

                if (dependencies != null && !dependencies.isEmpty()) {
                    if (!completedScripts.containsAll(dependencies)) {
                        context.addErrorLog(
                            String.format("Script %d in wave %d is scheduled before its dependencies %s.",
                            script.getScriptId(), i, dependencies)
                        );
                    }
                }
            }

            wave.scripts().forEach(s -> completedScripts.add(s.getScriptId()));
        }
        chain.doNextAfter(context, plan);
    }
}