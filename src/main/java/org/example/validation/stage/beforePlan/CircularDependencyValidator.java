package org.example.validation.stage.beforePlan;

import org.example.graph.DependencyGraph;
import org.example.validation.ValidationContext;
import org.example.validation.chain.ValidationChain;
import org.example.validation.stage.ValidatorStage;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

@Component
public class CircularDependencyValidator implements ValidatorStage {

    @Override
    public void executeValidationBeforePlan(ValidationContext context, ValidationChain chain) {
        DependencyGraph graph = DependencyGraph.buildGraph(context.scripts());
        Map<Integer, Integer> currentInDegrees = new HashMap<>();
        Queue<Integer> queue = new LinkedList<>();

        for (Integer scriptId : graph.getAllVertexIds()) {
            int inDegree = graph.getInDegree(scriptId);
            currentInDegrees.put(scriptId, inDegree);

            if (inDegree == 0) {
                queue.offer(scriptId);
            }
        }

        int processedCount = 0;
        while (!queue.isEmpty()) {
            int node = queue.poll();
            processedCount++;

            for (Integer neighbor : graph.getAdjacentVertices(node)) {
                int newDegree = currentInDegrees.get(neighbor) - 1;
                currentInDegrees.put(neighbor, newDegree);

                if (newDegree == 0) {
                    queue.offer(neighbor);
                }
            }
        }

        if (processedCount != graph.getAllVertexIds().size()) {
            context.addErrorLog("The execution graph contains an unresolvable cycle.");
            return;
        }

        chain.doNextBefore(context);
    }
}
