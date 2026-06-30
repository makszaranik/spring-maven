package org.example.graph;

import lombok.Builder;
import org.example.domain.VulnerabilityScript;
import org.jspecify.annotations.NonNull;

import java.util.*;

public class DependencyGraphUtils {

    public static @NonNull List<List<Integer>> findAllCycles(@NonNull DependencyGraph graph) {
        Map<Integer, Integer> inDegree = new HashMap<>();
        Queue<Integer> queue = new LinkedList<>();
        Set<Integer> processed = new HashSet<>();

        for (Integer vertex : graph.getAllVertexIds()) {
            int in = graph.getInDegree(vertex);
            inDegree.put(vertex, in);
            if (in == 0) {
                queue.offer(vertex);
            }
        }

        while (!queue.isEmpty()) {
            int u = queue.poll();
            processed.add(u);

            for (Integer v : graph.getAdjacentVertices(u)) {
                int currentInDegree = inDegree.get(v) - 1;
                inDegree.put(v, currentInDegree);
                if (currentInDegree == 0) {
                    queue.offer(v);
                }
            }
        }

        List<Integer> cyclicNodes = new ArrayList<>();
        for (Integer vertex : graph.getAllVertexIds()) {
            if (!processed.contains(vertex)) {
                cyclicNodes.add(vertex);
            }
        }

        return cyclicNodes.isEmpty() ? Collections.emptyList() : List.of(cyclicNodes);
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
