package org.example.validation.stage.before;

import lombok.extern.slf4j.Slf4j;
import org.example.domain.VulnerabilityScript;
import org.example.validation.ValidationContext;
import org.example.validation.chain.ValidationChain;
import org.example.validation.stage.ValidatorStage;
import org.jspecify.annotations.NonNull;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Order(3)
@Component
public class MissingDependencyValidator implements ValidatorStage {

    @Override
    public void executeValidationBeforePlan(@NonNull ValidationContext context, @NonNull ValidationChain chain) {
        List<VulnerabilityScript> scripts = context.validScripts();
        Set<Integer> allScriptsIds = scripts.stream().map(VulnerabilityScript::getScriptId).collect(Collectors.toSet());

        Map<Integer, List<Integer>> dependents = new HashMap<>();
        Queue<Integer> queue = new LinkedList<>();

        for (VulnerabilityScript script : scripts) {
            if (script.getDependencies() != null) {
                for (Integer depId : script.getDependencies()) {
                    if (!allScriptsIds.contains(depId)) {
                        queue.add(script.getScriptId());
                        context.addErrorLog(String.format("Script %d contains missing dependency: %d", script.getScriptId(), depId));
                    } else {
                        dependents.computeIfAbsent(depId, k -> new ArrayList<>()).add(script.getScriptId());
                    }
                }
            }
        }

        Set<Integer> toRemove = new HashSet<>();
        while (!queue.isEmpty()) {
            Integer removedId = queue.poll();
            if (toRemove.add(removedId)) {
                if (dependents.containsKey(removedId)) {
                    List<Integer> dependentScripts = dependents.get(removedId);
                    queue.addAll(dependentScripts);
                    for (Integer dependentId : dependentScripts) {
                        if (!toRemove.contains(dependentId)) {
                            context.addErrorLog(String.format("Script %d excluded due to transitive dependency on removed script %d", dependentId, removedId));
                        }
                    }
                }
            }
        }

        if (!toRemove.isEmpty()) {
            scripts.removeIf(s -> toRemove.contains(s.getScriptId()));
            log.info("MissingDependencyValidator: removed {} scripts due to dependencies.", toRemove.size());
        }

        log.info("MissingDependencyValidator completed");
        chain.doNext(chain, context);
    }
}