package org.example.validation.stage.after;

import org.example.domain.VulnerabilityScript;
import org.example.execution.plan.ExecutionPlan;
import org.example.validation.ValidationContext;
import org.example.validation.chain.ValidationChain;
import org.example.validation.stage.ValidatorStage;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class ScriptAppearsOnceValidator implements ValidatorStage {

    @Override
    public void executeValidationAfterPlan(@NonNull ValidationContext context, @NonNull ValidationChain chain, @NonNull ExecutionPlan plan) {
        Set<Integer> seenScripts = new HashSet<>();

        for (ExecutionPlan.ExecutionWave wave : plan.waves()) {
            for (VulnerabilityScript script : wave.scripts()) {
                if (!seenScripts.add(script.getScriptId())) {
                    context.warnings().add(String.format("Script %s appears more then once", script.getScriptId()));
                    return;
                }
            }
        }
        chain.doNext(chain, context, plan);
    }
}
