package org.example.validation.stage.before;

import lombok.extern.slf4j.Slf4j;
import org.example.domain.VulnerabilityScript;
import org.example.validation.ValidationContext;
import org.example.validation.chain.ValidationChain;
import org.example.validation.stage.ValidatorStage;
import org.jspecify.annotations.NonNull;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Order(2)
@Component
public class SelfDependencyValidator implements ValidatorStage {

    @Override
    public void executeValidationBeforePlan(@NonNull ValidationContext context, @NonNull ValidationChain chain) {
        for (VulnerabilityScript script : context.validScripts()) {
            List<Integer> deps = script.getDependencies();
            if (deps != null && deps.contains(script.getScriptId())) {
                log.warn("Script {} depends on itself. Ignoring self-dependency.", script.getScriptId());
                context.addWarningLog(String.format("Script %s depends on itself. Self-dependency ignored.", script.getScriptId()));
                deps.removeIf(id -> id.equals(script.getScriptId()));
            }
        }

        log.info("SelfDependencyValidator finished");
        chain.doNext(chain, context);
    }
}
