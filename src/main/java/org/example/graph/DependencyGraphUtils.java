package org.example.graph;

import lombok.Builder;
import org.example.domain.VulnerabilityScript;
import org.jspecify.annotations.NonNull;

import java.util.*;

public class DependencyGraphUtils {

    public static @NonNull List<List<Integer>> findAllCycles(@NonNull DependencyGraph graph) {
        List<List<Integer>> cycles = new ArrayList<>();

        Map<Integer, Integer> state = new HashMap<>(); //0 - not visited, 1 - processing, 2 - finished
        Map<Integer, Integer> parent = new HashMap<>();

        for (Integer vertex : graph.getAllVertexIds()) {
            state.put(vertex, 0);
        }

        for (Integer startNode : graph.getAllVertexIds()) {
            if (state.get(startNode) == 0) {
                Deque<Integer> stack = new ArrayDeque<>();
                stack.push(startNode);

                while (!stack.isEmpty()) {
                    int current = stack.pop();

                    if (state.get(current) == 2) {
                        continue;
                    }

                    if (state.get(current) == 1) {
                        state.put(current, 2);
                        continue;
                    }

                    state.put(current, 1);
                    stack.push(current);

                    for (Integer neighbor : graph.getAdjacentVertices(current)) {
                        int neighborState = state.getOrDefault(neighbor, 0);

                        if (neighborState == 0) {
                            parent.put(neighbor, current);
                            stack.push(neighbor);
                        }
                        else if (neighborState == 1) { // vertex with 1 status - cycle
                            List<Integer> cycle = new ArrayList<>();
                            cycle.add(neighbor);

                            int p = current;
                            boolean isValid = true;

                            //find cycle path
                            while (p != neighbor) {
                                cycle.add(p);
                                if (!parent.containsKey(p)) {
                                    isValid = false;
                                    break;
                                }
                                p = parent.get(p);
                            }

                            if (isValid) {
                                cycle.add(neighbor);
                                Collections.reverse(cycle);
                                cycles.add(cycle);
                            }
                        }
                    }
                }
            }
        }

        return cycles;
    }

    public static @NonNull CriticalPath calculateCriticalPath(@NonNull DependencyGraph graph) {
        Map<Integer, CriticalPath> used = new HashMap<>();

        CriticalPath maxPath = CriticalPath.builder()
            .vertexes(Collections.emptyList())
            .duration(0)
            .build();

        int totalExecutionTimeAllScripts = 0;

        for (Integer vertex : graph.getAllVertexIds()) {
            VulnerabilityScript script = graph.getVertex(vertex);
            if (script != null) {
                totalExecutionTimeAllScripts += script.getEstimatedDurationSeconds();
            }
        }

        for (Integer vertex : graph.getAllVertexIds()) {
            CriticalPath path = findLongestPath(graph, vertex, used);
            if (path.duration() > maxPath.duration()) {
                maxPath = path;
            }
        }

        double executionTimePercentage = executionTimePercentage(totalExecutionTimeAllScripts, maxPath);

        return CriticalPath.builder()
            .vertexes(maxPath.vertexes())
            .duration(maxPath.duration())
            .percentage(executionTimePercentage)
            .build();

    }

    private static double executionTimePercentage(int totalExecutionTimeAllScripts, @NonNull CriticalPath maxPath) {
        return totalExecutionTimeAllScripts == 0 ? 0.0 :
            ((double) maxPath.duration() / totalExecutionTimeAllScripts) * 100.0;
    }

    private static @NonNull CriticalPath findLongestPath(@NonNull DependencyGraph graph, int startVertex, @NonNull Map<Integer, CriticalPath> used) {
        Deque<Integer> stack = new ArrayDeque<>();
        stack.push(startVertex);

        while (!stack.isEmpty()) {
            int current = stack.peek();

            if (used.containsKey(current)) {
                stack.pop();
                continue;
            }

            boolean allChildrenProcessed = true;
            for (Integer child : graph.getAdjacentVertices(current)) {
                if (!used.containsKey(child)) {
                    stack.push(child);
                    allChildrenProcessed = false;
                }
            }

            if (!allChildrenProcessed) {
                continue;
            }

            stack.pop();

            VulnerabilityScript script = graph.getVertex(current);
            int durationSeconds = script != null ? script.getEstimatedDurationSeconds() : 0;

            CriticalPath bestChildPath = CriticalPath.builder()
                .vertexes(Collections.emptyList())
                .duration(0)
                .build();

            for (Integer child : graph.getAdjacentVertices(current)) {
                CriticalPath childPath = used.get(child);
                if (childPath != null && childPath.duration() > bestChildPath.duration()) {
                    bestChildPath = childPath;
                }
            }

            List<Integer> combinedNodes = new ArrayList<>();
            combinedNodes.add(current);
            combinedNodes.addAll(bestChildPath.vertexes());

            used.put(current, CriticalPath.builder()
                .vertexes(combinedNodes)
                .duration(durationSeconds + bestChildPath.duration())
                .build());
        }

        return used.get(startVertex);
    }

    @Builder
    public record CriticalPath(
        List<Integer> vertexes,
        int duration,
        double percentage
    ) {
    }

}
