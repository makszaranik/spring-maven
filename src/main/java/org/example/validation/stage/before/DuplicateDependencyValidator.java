package org.example.validation.stage.before;

import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Order(1)
@Component
public class DuplicateDependencyValidator implements ValidatorStage {

    @Override
    public void executeValidationBeforePlan(@NonNull ValidationContext context, @NonNull ValidationChain chain) {
        for (VulnerabilityScript script : context.validScripts()) {
            List<Integer> deps = script.getDependencies();
            if (deps != null && !deps.isEmpty()) {
                Set<Integer> uniqueDeps = new HashSet<>(deps);
                if (uniqueDeps.size() < deps.size()) {
                    log.info("Script {} contains duplicated dependencies", script.getScriptId());
                    context.addWarningLog(String.format("Script %s contains duplicated dependencies", script.getScriptId()));
                    script.setDependencies(new ArrayList<>(uniqueDeps));
                }
            }
        }

        log.info("duplicate dependency validator");
        chain.doNext(chain, context);
    }
}
