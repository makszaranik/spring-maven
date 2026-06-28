package org.example.validation.stage.before;

import org.example.graph.DependencyGraph;
import org.example.graph.DependencyGraphUtils;
import org.example.validation.ValidationContext;
import org.example.validation.chain.ValidationChain;
import org.example.validation.stage.ValidatorStage;
import org.jspecify.annotations.NonNull;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.*;

@Order(4)
@Component
public class CircularDependencyValidator implements ValidatorStage {

    @Override
    public void executeValidationBeforePlan(@NonNull ValidationContext context, @NonNull ValidationChain chain) {
        DependencyGraph graph = DependencyGraph.buildGraph(context.scripts());
        List<List<Integer>> cycles = DependencyGraphUtils.findAllCycles(graph);

        if (!cycles.isEmpty()) {
            for (List<Integer> cycle : cycles) {
                context.addErrorLog("The execution graph contains an unresolvable cycle: " + cycle);
            }
            return;
        }

        chain.doNext(chain, context);
    }
}
