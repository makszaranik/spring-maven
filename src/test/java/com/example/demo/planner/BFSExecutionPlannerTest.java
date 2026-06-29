package com.example.demo.planner;

import org.example.config.PlannerConfig;
import org.example.domain.VulnerabilityScript;
import org.example.execution.plan.BFSExecutionPlanner;
import org.example.execution.plan.ExecutionPlan;
import org.example.sheduling.BaseSchedulingOrderStrategy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {
    BFSExecutionPlanner.class,
    org.example.sheduling.BaseSchedulingOrderStrategy.class
})
class BFSExecutionPlannerTest {

    @Autowired
    private BFSExecutionPlanner executionPlanner;

    @MockitoBean
    private PlannerConfig plannerConfig;

    private VulnerabilityScript createScript(int id, VulnerabilityScript.Priority priority, int duration, Integer... deps) {
        return VulnerabilityScript.builder()
            .scriptId(id)
            .dependencies(new ArrayList<>(Arrays.asList(deps)))
            .estimatedDurationSeconds(duration)
            .priority(priority)
            .build();
    }

    @Test
    void shouldPlanSimpleDagCorrectly() {
        when(plannerConfig.maxParallelExecutions()).thenReturn(2);
        List<VulnerabilityScript> scripts = List.of(
            createScript(1, VulnerabilityScript.Priority.MEDIUM, 10),
            createScript(2, VulnerabilityScript.Priority.MEDIUM, 10, 1),
            createScript(3, VulnerabilityScript.Priority.MEDIUM, 10, 2)
        );

        ExecutionPlan executionPlan = executionPlanner.createPlan(scripts);

        assertEquals(3, executionPlan.waves().size());
        assertEquals(1, executionPlan.waves().getFirst().scripts().getFirst().getScriptId());
        assertEquals(2, executionPlan.waves().get(1).scripts().getFirst().getScriptId());
        assertEquals(3, executionPlan.waves().get(2).scripts().getFirst().getScriptId());
    }

    @Test
    void shouldRespectParallelismLimits() {
        when(plannerConfig.maxParallelExecutions()).thenReturn(2);
        List<VulnerabilityScript> scripts = List.of(
            createScript(1, VulnerabilityScript.Priority.MEDIUM, 1),
            createScript(2, VulnerabilityScript.Priority.MEDIUM, 1),
            createScript(3, VulnerabilityScript.Priority.MEDIUM, 1),
            createScript(4, VulnerabilityScript.Priority.MEDIUM, 1)
        );

        ExecutionPlan plan = executionPlanner.createPlan(scripts);

        assertEquals(2, plan.waves().size());
        assertEquals(2, plan.waves().get(0).scripts().size());
        assertEquals(2, plan.waves().get(1).scripts().size());
    }

    @Test
    void shouldRespectPriorityScheduling() {
        when(plannerConfig.maxParallelExecutions()).thenReturn(3);
        List<VulnerabilityScript> scripts = List.of(
            createScript(0, VulnerabilityScript.Priority.MEDIUM, 10),
            createScript(1, VulnerabilityScript.Priority.HIGH, 15, 0),
            createScript(2, VulnerabilityScript.Priority.MEDIUM, 40, 0),
            createScript(3, VulnerabilityScript.Priority.MEDIUM, 50, 0)
        );

        ExecutionPlan executionPlan = executionPlanner.createPlan(scripts);

        assertEquals(2, executionPlan.waves().size());
        assertEquals(0, executionPlan.waves().getFirst().scripts().getFirst().getScriptId());
        assertEquals(1, executionPlan.waves().get(1).scripts().getFirst().getScriptId());
        assertEquals(3, executionPlan.waves().get(1).scripts().get(1).getScriptId());
        assertEquals(2, executionPlan.waves().get(1).scripts().get(2).getScriptId());
    }

    @Test
    void shouldPlanWideDependencyGraphWithLimits() {
        when(plannerConfig.maxParallelExecutions()).thenReturn(5);
        List<VulnerabilityScript> scripts = new ArrayList<>();
        scripts.add(createScript(0, VulnerabilityScript.Priority.MEDIUM, 10));

        for (int i = 1; i <= 10; i++) {
            scripts.add(createScript(i, VulnerabilityScript.Priority.MEDIUM, 10, 0));
        }

        List<Integer> lastScriptDeps = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            lastScriptDeps.add(i);
        }

        scripts.add(createScript(99, VulnerabilityScript.Priority.MEDIUM, 10, lastScriptDeps.toArray(new Integer[0])));

        ExecutionPlan executionPlan = executionPlanner.createPlan(scripts);

        assertEquals(4, executionPlan.waves().size());
        assertEquals(1, executionPlan.waves().get(0).scripts().size());
        assertEquals(5, executionPlan.waves().get(1).scripts().size());
        assertEquals(5, executionPlan.waves().get(2).scripts().size());
        assertEquals(1, executionPlan.waves().get(3).scripts().size());
        assertEquals(0, executionPlan.waves().get(0).scripts().getFirst().getScriptId());
        assertEquals(99, executionPlan.waves().get(3).scripts().getFirst().getScriptId());
    }

    @Test
    void shouldHandleMassiveDataset() {
        PlannerConfig config = new PlannerConfig(100);
        BFSExecutionPlanner fastPlanner = new BFSExecutionPlanner(new BaseSchedulingOrderStrategy(), config);
        int scriptCount = 1_000_000;
        List<VulnerabilityScript> scripts = new ArrayList<>(scriptCount);

        for (int i = 0; i < scriptCount; i++) {
            List<Integer> deps = (i == 0) ? new ArrayList<>() : List.of(i - 1);
            scripts.add(VulnerabilityScript.builder()
                .scriptId(i)
                .dependencies(deps)
                .estimatedDurationSeconds(1)
                .priority(VulnerabilityScript.Priority.MEDIUM)
                .build());
        }

        assertTimeoutPreemptively(java.time.Duration.ofSeconds(10), () -> {
            ExecutionPlan plan = fastPlanner.createPlan(scripts);
            assertNotNull(plan);
            assertEquals(scriptCount, plan.waves().size());
        });
    }

    @Test
    void shouldHandleIncrementalReplanning() {
        when(plannerConfig.maxParallelExecutions()).thenReturn(5);
        List<VulnerabilityScript> existingScripts = new ArrayList<>(List.of(
            createScript(1, VulnerabilityScript.Priority.MEDIUM, 10),
            createScript(2, VulnerabilityScript.Priority.MEDIUM, 10, 1)
        ));

        ExecutionPlan initialPlan = executionPlanner.createPlan(existingScripts);

        assertEquals(2, initialPlan.waves().size());

        VulnerabilityScript newScript = createScript(3, VulnerabilityScript.Priority.HIGH, 10, 2);
        existingScripts.add(newScript);

        ExecutionPlan updatedPlan = executionPlanner.createPlan(existingScripts);

        assertEquals(3, updatedPlan.waves().size());
        assertEquals(1, updatedPlan.waves().get(0).scripts().getFirst().getScriptId());
        assertEquals(2, updatedPlan.waves().get(1).scripts().getFirst().getScriptId());
        assertEquals(3, updatedPlan.waves().get(2).scripts().getFirst().getScriptId());
    }
}