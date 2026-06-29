package org.example.validation.stage.before;

import lombok.extern.slf4j.Slf4j;
import org.example.domain.VulnerabilityScript;
import org.example.graph.DependencyGraph;
import org.example.graph.DependencyGraphUtils;
import org.example.validation.ValidationContext;
import org.example.validation.chain.ValidationChain;
import org.example.validation.stage.ValidatorStage;
import org.jspecify.annotations.NonNull;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Order(4)
@Component
public class CircularDependencyValidator implements ValidatorStage {

    @Override
    public void executeValidationBeforePlan(@NonNull ValidationContext context, @NonNull ValidationChain chain) {
        List<VulnerabilityScript> scripts = context.validScripts();
        DependencyGraph graph = DependencyGraph.buildGraph(scripts);
        List<List<Integer>> cycles = DependencyGraphUtils.findAllCycles(graph);

        if (!cycles.isEmpty()) {
            Set<Integer> scriptsInCycles = new HashSet<>();
            for (List<Integer> cycle : cycles) {
                scriptsInCycles.addAll(cycle);
                context.addErrorLog("The execution graph contains an unresolvable cycle: " + cycle);
                log.error("Cycle detected: {}", cycle);
            }

            scripts.removeIf(s -> scriptsInCycles.contains(s.getScriptId()));
            log.info("CircularDependencyValidator: removed {} scripts due to cyclic dependencies.", scriptsInCycles.size());
        }

        chain.doNext(chain, context);
    }
}