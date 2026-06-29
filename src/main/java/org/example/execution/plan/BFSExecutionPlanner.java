package org.example.execution.plan;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.config.PlannerConfig;
import org.example.domain.VulnerabilityScript;
import org.example.graph.DependencyGraph;
import org.example.sheduling.SchedulingOrderStrategy;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.util.*;

@Data
@Component
@RequiredArgsConstructor
public class BFSExecutionPlanner implements ExecutionPlanner {

    private final SchedulingOrderStrategy orderStrategy;
    private final PlannerConfig plannerConfig;

    @Override
    public @NonNull ExecutionPlan createPlan(@NonNull Collection<VulnerabilityScript> scripts) {
        DependencyGraph graph = DependencyGraph.buildGraph(scripts);
        Map<Integer, Integer> currentInDegrees = new HashMap<>();
        Queue<VulnerabilityScript> readyScripts = new PriorityQueue<>(orderStrategy);
        Set<Integer> processedScripts = new HashSet<>();

        for (Integer scriptId : graph.getAllVertexIds()) {
            int inDegree = graph.getInDegree(scriptId);
            currentInDegrees.put(scriptId, inDegree);
            if (inDegree == 0) { //should start with 0 inDegree vertexes
                readyScripts.offer(graph.getVertex(scriptId));
            }
        }

        List<ExecutionPlan.ExecutionWave> waves = new ArrayList<>();
        while (!readyScripts.isEmpty()) {
            int waveSize = Math.min(readyScripts.size(), plannerConfig.maxParallelExecutions());
            List<VulnerabilityScript> currentWaveScripts = new ArrayList<>(waveSize);

            for (int i = 0; i < waveSize; i++) {
                currentWaveScripts.add(readyScripts.poll());
            }

            for (VulnerabilityScript executedScript : currentWaveScripts) {
                int currentVertexId = executedScript.getScriptId();
                processedScripts.add(currentVertexId);

                for (Integer dependentVertexId : graph.getAdjacentVertices(currentVertexId)) {
                    int newInDegree = currentInDegrees.get(dependentVertexId) - 1;
                    currentInDegrees.put(dependentVertexId, newInDegree);

                    if (newInDegree == 0 && !processedScripts.contains(dependentVertexId)) {
                        readyScripts.offer(graph.getVertex(dependentVertexId));
                    }
                }
            }

            waves.add(new ExecutionPlan.ExecutionWave(currentWaveScripts));
        }

        return new ExecutionPlan(waves);
    }


    @Override
    public void addScript(@NonNull ExecutionPlan plan, @NonNull VulnerabilityScript script) {
        List<ExecutionPlan.ExecutionWave> waves = plan.waves();
        List<Integer> dependencies = script.getDependencies() == null ? Collections.emptyList() : script.getDependencies();

        int targetWaveIndex = -1;

        //script should appear in at least (maximal + 1) wave of all script dependencies
        for (int i = 0; i < waves.size(); i++) {
            ExecutionPlan.ExecutionWave currentWave = waves.get(i);
            boolean containsDependencyInWave = currentWave.scripts().stream()
                .map(VulnerabilityScript::getScriptId)
                .anyMatch(dependencies::contains);

            if (containsDependencyInWave) {
                targetWaveIndex = Math.max(targetWaveIndex, i);
            }
        }

        //wave exists, script must appear in next available wave
        if (targetWaveIndex != -1) targetWaveIndex++;

        //find next available wave
        while (targetWaveIndex < waves.size() && targetWaveIndex >= 0) {
            if (waves.get(targetWaveIndex).scripts().size() < plannerConfig.maxParallelExecutions()) {
                break;
            }
            targetWaveIndex++;
        }

        //insert script in wave or create a new one
        if (targetWaveIndex < waves.size() && targetWaveIndex >= 0) {
            waves.get(targetWaveIndex).scripts().add(script);
        } else {
            List<VulnerabilityScript> newWave = new ArrayList<>();
            newWave.add(script);
            waves.add(new ExecutionPlan.ExecutionWave(newWave));
        }
    }
}