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

        //given
        when(plannerConfig.maxParallelExecutions()).thenReturn(2);

        List<VulnerabilityScript> scripts = List.of(
            createScript(1, VulnerabilityScript.Priority.MEDIUM, 10),
            createScript(2, VulnerabilityScript.Priority.MEDIUM, 10, 1),
            createScript(3, VulnerabilityScript.Priority.MEDIUM, 10, 2)
        );

        //when
        ExecutionPlan executionPlan = executionPlanner.createPlan(scripts);

        System.out.println(executionPlan);

        //expected
        assertEquals(3, executionPlan.waves().size());
        assertEquals(1, executionPlan.waves().getFirst().scripts().getFirst().getScriptId());
        assertEquals(2, executionPlan.waves().get(1).scripts().getFirst().getScriptId());
        assertEquals(3, executionPlan.waves().get(2).scripts().getFirst().getScriptId());

    }

    @Test
    void shouldRespectParallelismLimits() {

        //given
        when(plannerConfig.maxParallelExecutions()).thenReturn(2);

        List<VulnerabilityScript> scripts = List.of(
            createScript(1, VulnerabilityScript.Priority.MEDIUM, 1),
            createScript(2, VulnerabilityScript.Priority.MEDIUM, 1),
            createScript(3, VulnerabilityScript.Priority.MEDIUM, 1),
            createScript(4, VulnerabilityScript.Priority.MEDIUM, 1)
        );

        //when
        ExecutionPlan plan = executionPlanner.createPlan(scripts);

        //expected
        assertEquals(2, plan.waves().size());
        assertEquals(2, plan.waves().get(0).scripts().size());
        assertEquals(2, plan.waves().get(1).scripts().size());
    }

    @Test
    void shouldRespectPriorityScheduling() {

        //given
        when(plannerConfig.maxParallelExecutions()).thenReturn(3);
        List<VulnerabilityScript> scripts = List.of(
            createScript(0, VulnerabilityScript.Priority.MEDIUM, 10),
            createScript(1, VulnerabilityScript.Priority.HIGH, 15, 0),
            createScript(2, VulnerabilityScript.Priority.MEDIUM, 40, 0),
            createScript(3, VulnerabilityScript.Priority.MEDIUM, 50, 0)
        );

        //when
        ExecutionPlan executionPlan = executionPlanner.createPlan(scripts);

        //expected
        assertEquals(2, executionPlan.waves().size());
        assertEquals(0, executionPlan.waves().getFirst().scripts().getFirst().getScriptId());
        assertEquals(1, executionPlan.waves().get(1).scripts().getFirst().getScriptId());
        assertEquals(3, executionPlan.waves().get(1).scripts().get(1).getScriptId());
        assertEquals(2, executionPlan.waves().get(1).scripts().get(2).getScriptId());
    }

    @Test
    void shouldPlanWideDependencyGraphWithLimits() {

        //given
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

        VulnerabilityScript lastScript = createScript(
            99, VulnerabilityScript.Priority.MEDIUM,
            10, lastScriptDeps.toArray(new Integer[0])
        );

        scripts.add(lastScript);

        //when
        ExecutionPlan executionPlan = executionPlanner.createPlan(scripts);

        //expected
        assertEquals(4, executionPlan.waves().size(), "should be 4 waves");

        assertEquals(1, executionPlan.waves().get(0).scripts().size(), "should be 1 script");
        assertEquals(5, executionPlan.waves().get(1).scripts().size(), "should be 5 script");
        assertEquals(5, executionPlan.waves().get(2).scripts().size(), "should be 5 script");
        assertEquals(1, executionPlan.waves().get(3).scripts().size(), "should be 1 script");

        assertEquals(0, executionPlan.waves().get(0).scripts().getFirst().getScriptId());
        assertEquals(99, executionPlan.waves().get(3).scripts().getFirst().getScriptId());
    }


    @Test
    void shouldHandleMassiveDataset() {

        //given
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

        //when
        assertTimeoutPreemptively(java.time.Duration.ofSeconds(10), () -> {
            ExecutionPlan plan = fastPlanner.createPlan(scripts);

            //expected
            assertNotNull(plan);
            assertEquals(scriptCount, plan.waves().size());
        });
    }

}