package org.example.validation.stage.after;

import lombok.extern.slf4j.Slf4j;
import org.example.domain.VulnerabilityScript;
import org.example.execution.plan.ExecutionPlan;
import org.example.validation.ValidationContext;
import org.example.validation.chain.ValidationChain;
import org.example.validation.stage.ValidatorStage;
import org.jspecify.annotations.NonNull;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@Order(5)
@Component
public class ScriptAppearsOnceValidator implements ValidatorStage {

    @Override
    public void executeValidationAfterPlan(@NonNull ValidationContext context, @NonNull ValidationChain chain, @NonNull ExecutionPlan plan) {
        Set<Integer> seenScripts = new HashSet<>();
        int plannedCount = 0;

        //duplicate scripts
        for (ExecutionPlan.ExecutionWave wave : plan.waves()) {
            for (VulnerabilityScript script : wave.scripts()) {
                plannedCount++;
                if (!seenScripts.add(script.getScriptId())) {
                    log.error("Script {} appears more than once in the execution plan", script.getScriptId());
                    context.addErrorLog(String.format("Script %s appears more than once in the plan", script.getScriptId()));
                }
            }
        }

        //lost scripts
        if (plannedCount != context.validScripts().size()) {
            log.error("Plan size mismatch: expected {} scripts, but planned {}", context.validScripts().size(), plannedCount);
            context.addErrorLog(String.format("Planner lost or duplicated scripts. Context valid scripts: %d, Planned: %d", context.validScripts().size(), plannedCount));
        }

        log.info("ScriptAppearsOnceValidator completed");
        chain.doNext(chain, context, plan);
    }
}