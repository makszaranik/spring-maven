package org.example.graph;

import lombok.Builder;

import java.util.*;

public class DependencyGraphUtils {

    public static List<List<Integer>> findAllCycles(DependencyGraph graph) {
        List<List<Integer>> allCycles = new ArrayList<>();
        Map<Integer, Integer> visited = new HashMap<>(); // 0 - not visited, 1 - processing, 2 - finished
        List<Integer> currentPath = new ArrayList<>();

        for (Integer vertex : graph.getAllVertexIds()) {
            visited.put(vertex, 0);
        }

        for (Integer vertex : graph.getAllVertexIds()) {
            if (visited.get(vertex) == 0) {
                dfs(graph, vertex, visited, currentPath, allCycles);
            }
        }

        return allCycles;
    }


    private static void dfs(DependencyGraph graph, int v, Map<Integer, Integer> visited,
                            List<Integer> currentPath, List<List<Integer>> allCycles) {

        visited.put(v, 1);
        currentPath.add(v);

        for (Integer to : graph.getAdjacentVertices(v)) {
            int visitedState = visited.getOrDefault(to, 0);

            if (visitedState == 0) {
                dfs(graph, to, visited, currentPath, allCycles);
            } else if (visitedState == 1) {
                int cycleStartIndex = currentPath.indexOf(to);
                List<Integer> cycle = new ArrayList<>(currentPath.subList(cycleStartIndex, currentPath.size()));
                cycle.add(to);
                allCycles.add(cycle);
            }

        }

        visited.put(v, 2);
        currentPath.removeLast();
    }

    public static CriticalPath calculateCriticalPath(DependencyGraph graph) {
        Map<Integer, CriticalPath> used = new HashMap<>();

        CriticalPath maxPath = CriticalPath.builder()
            .vertexes(Collections.emptyList())
            .duration(0)
            .build();

        int totalExecutionTimeAllScripts = 0;

        for (Integer vertex : graph.getAllVertexIds()) {
            totalExecutionTimeAllScripts += graph.getVertex(vertex).getEstimatedDurationSeconds();
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

    private static double executionTimePercentage(int totalExecutionTimeAllScripts, CriticalPath maxPath) {
        return totalExecutionTimeAllScripts == 0 ? 0.0 :
            ((double) maxPath.duration() / totalExecutionTimeAllScripts) * 100.0;
    }

    private static CriticalPath findLongestPath(DependencyGraph graph, int currentVertex, Map<Integer, CriticalPath> used) {

        if (used.containsKey(currentVertex)) {
            return used.get(currentVertex);
        }

        int durationSeconds = graph.getVertex(currentVertex).getEstimatedDurationSeconds();

        CriticalPath bestChildPath = CriticalPath.builder()
            .vertexes(Collections.emptyList())
            .duration(0)
            .build();

        for (Integer child : graph.getAdjacentVertices(currentVertex)) {
            CriticalPath childPath = findLongestPath(graph, child, used);
            if (childPath.duration() > bestChildPath.duration()) {
                bestChildPath = childPath;
            }
        }

        List<Integer> combinedNodes = new ArrayList<>();
        combinedNodes.add(currentVertex);
        combinedNodes.addAll(bestChildPath.vertexes());

        CriticalPath result = CriticalPath.builder()
            .vertexes(combinedNodes)
            .duration(durationSeconds + bestChildPath.duration())
            .build();

        used.put(currentVertex, result);
        return result;
    }


    @Builder
    public record CriticalPath(
        List<Integer> vertexes,
        int duration,
        double percentage
    ) {}

}
