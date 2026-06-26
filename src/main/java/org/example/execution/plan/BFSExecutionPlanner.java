package org.example.execution.plan;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.config.PlannerConfig;
import org.example.domain.VulnerabilityScript;
import org.example.graph.DependencyGraph;
import org.example.sheduling.SchedulingOrderStrategy;
import org.example.validation.ValidationResult;
import org.springframework.stereotype.Component;

import java.util.*;

@Data
@Component
@RequiredArgsConstructor
public class BFSExecutionPlanner implements ExecutionPlanner {

    private final SchedulingOrderStrategy orderStrategy;
    private final PlannerConfig plannerConfig;

    @Override
    public ExecutionPlan createPlan(Collection<VulnerabilityScript> scripts) {
        DependencyGraph graph = DependencyGraph.buildGraph(scripts);
        Map<Integer, Integer> currentInDegrees = new HashMap<>();
        Queue<VulnerabilityScript> readyScripts = new PriorityQueue<>(orderStrategy);

        for (Integer scriptId : graph.getAllVertexIds()) {
            int inDegree = graph.getInDegree(scriptId);
            currentInDegrees.put(scriptId, inDegree);
            if (inDegree == 0) { //should start with 0 inDegree vertexes
                readyScripts.offer(graph.getVertex(scriptId));
            }
        }

        List<ExecutionPlan.ExecutionWave> waves = new ArrayList<>();

        while (!readyScripts.isEmpty()) {
            int waveSize = readyScripts.size();
            List<VulnerabilityScript> currentWaveScripts = new ArrayList<>(waveSize);

            for (int i = 0; i < waveSize; i++) {
                currentWaveScripts.add(readyScripts.poll());
            }

            for (VulnerabilityScript executedScript : currentWaveScripts) {
                int currentVertexId = executedScript.getScriptId();

                for (Integer dependentVertexId : graph.getAdjacentVertices(currentVertexId)) {
                    int newInDegree = currentInDegrees.get(dependentVertexId) - 1;
                    currentInDegrees.put(dependentVertexId, newInDegree);

                    if (newInDegree == 0) {
                        readyScripts.offer(graph.getVertex(dependentVertexId));
                    }
                }
            }

            waves.add(new ExecutionPlan.ExecutionWave(currentWaveScripts));
        }

        return new ExecutionPlan(waves);
    }

    @Override
    public ValidationResult validate(ExecutionPlan plan) {
        return null;
    }

    @Override
    public void addScript(ExecutionPlan plan, VulnerabilityScript script) {
        List<ExecutionPlan.ExecutionWave> waves = plan.waves();
        List<Integer> dependencies = script.getDependencies() == null ? Collections.emptyList() : script.getDependencies();

        int targetWaveIndex = 0;

    }
}