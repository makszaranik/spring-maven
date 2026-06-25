package org.example.graph;

import org.example.domain.VulnerabilityScript;

import java.util.*;

public class DependencyGraph {

    private final Map<Integer, List<Integer>> adjacencyList = new HashMap<>();
    private final Map<Integer, VulnerabilityScript> vertices = new HashMap<>();
    private final Map<Integer, Integer> inDegrees = new HashMap<>();

    public static DependencyGraph buildGraph(Collection<VulnerabilityScript> scripts) {
        DependencyGraph graph = new DependencyGraph();

        for (VulnerabilityScript script : scripts) {
            graph.addVertex(script);
        }

        for (VulnerabilityScript script : scripts) {
            int childId = script.getScriptId();
            List<Integer> dependencies = script.getDependencies();

            if (dependencies != null) {
                for (Integer parentId : dependencies) {
                    graph.addEdge(parentId, childId);
                }
            }
        }

        return graph;
    }

    public void addEdge(int parentId, int childId) {
        if (!vertices.containsKey(parentId) || !vertices.containsKey(childId)) {
            throw new IllegalArgumentException("Vertex not found");
        }
        adjacencyList.get(parentId).add(childId);
        inDegrees.put(childId, inDegrees.get(childId) + 1);
    }

    public void addVertex(VulnerabilityScript script) {
        int scriptId = script.getScriptId();
        vertices.putIfAbsent(scriptId, script);
        adjacencyList.putIfAbsent(scriptId, new ArrayList<>());
        inDegrees.putIfAbsent(scriptId, 0);
    }

    public List<Integer> getAdjacentVertices(int vertexId) {
        return adjacencyList.getOrDefault(vertexId, Collections.emptyList());
    }

    public VulnerabilityScript getVertex(int vertexId) {
        return vertices.get(vertexId);
    }

    public Set<Integer> getAllVertexIds() {
        return vertices.keySet();
    }

    public int getInDegree(int vertexId) {
        return inDegrees.getOrDefault(vertexId, 0);
    }

}
