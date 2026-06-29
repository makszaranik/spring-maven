package com.example.demo;

import org.example.config.PlannerConfig;
import org.example.domain.VulnerabilityScript;
import org.example.execution.plan.BFSExecutionPlanner;
import org.example.execution.plan.ExecutionPlan;
import org.example.validation.ValidationContext;
import org.example.validation.chain.AfterPlanValidationChain;
import org.example.validation.chain.BeforePlanValidationChain;
import org.example.validation.stage.after.*;
import org.example.validation.stage.before.*;
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
    BeforePlanValidationChain.class,
    DuplicateDependencyValidator.class,
    SelfDependencyValidator.class,
    MissingDependencyValidator.class,
    CircularDependencyValidator.class,
    BFSExecutionPlanner.class,
    BaseSchedulingOrderStrategy.class,
    AfterPlanValidationChain.class,
    ScriptAppearsOnceValidator.class,
    DependencyOrderValidator.class,
    MinimalWavesValidator.class,
    ParallelismLimitValidator.class
})
public class PlannerPipelineE2ETest {

    @Autowired
    private BeforePlanValidationChain beforeValidationChain;

    @Autowired
    private BFSExecutionPlanner executionPlanner;

    @Autowired
    private AfterPlanValidationChain afterValidationChain;

    @MockitoBean
    private PlannerConfig plannerConfig;

    private VulnerabilityScript createScript(int id, Integer... deps) {
        return VulnerabilityScript.builder()
            .scriptId(id)
            .dependencies(new ArrayList<>(Arrays.asList(deps)))
            .estimatedDurationSeconds(10)
            .priority(VulnerabilityScript.Priority.MEDIUM)
            .build();
    }

    @Test
    void shouldExecuteFullPipelineWithPerfectData() {

        // given
        when(plannerConfig.maxParallelExecutions()).thenReturn(2);

        List<VulnerabilityScript> rawScripts = new ArrayList<>(List.of(
            createScript(1),
            createScript(2, 1),
            createScript(3, 1),
            createScript(4, 2, 3)
        ));

        ValidationContext context = new ValidationContext(rawScripts, new ArrayList<>(), new ArrayList<>());

        // when
        beforeValidationChain.startChain(context);

        // then
        assertTrue(context.errors().isEmpty());
        assertTrue(context.warnings().isEmpty());
        assertEquals(4, context.validScripts().size());

        // when
        ExecutionPlan plan = executionPlanner.createPlan(context.validScripts());

        // then
        assertNotNull(plan);
        assertEquals(3, plan.waves().size());
        assertEquals(1, plan.waves().get(0).scripts().size());
        assertEquals(2, plan.waves().get(1).scripts().size());
        assertEquals(1, plan.waves().get(2).scripts().size());

        // when
        afterValidationChain.startChain(context, plan);

        // then
        assertTrue(context.errors().isEmpty());
    }

    @Test
    void shouldHandleCompletelyBrokenDataGracefully() {

        // given
        when(plannerConfig.maxParallelExecutions()).thenReturn(5);

        List<VulnerabilityScript> rawScripts = new ArrayList<>(List.of(
            createScript(1, 2),
            createScript(2, 1),
            createScript(3, 99),
            createScript(4, 3)
        ));

        ValidationContext context = new ValidationContext(rawScripts, new ArrayList<>(), new ArrayList<>());

        // when
        beforeValidationChain.startChain(context);

        // then
        assertTrue(context.errors().size() >= 2);
        assertEquals(0, context.validScripts().size());

        // when
        ExecutionPlan plan = executionPlanner.createPlan(context.validScripts());

        // then
        assertNotNull(plan);
        assertTrue(plan.waves().isEmpty());

        // given
        context.errors().clear();

        // when
        afterValidationChain.startChain(context, plan);

        // then
        assertTrue(context.errors().isEmpty());
    }

    @Test
    void shouldEnforceStrictParallelismLimitsOnWideGraph() {

        // given
        when(plannerConfig.maxParallelExecutions()).thenReturn(3);

        List<VulnerabilityScript> rawScripts = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            rawScripts.add(createScript(i));
        }

        ValidationContext context = new ValidationContext(rawScripts, new ArrayList<>(), new ArrayList<>());

        // when
        beforeValidationChain.startChain(context);

        // then
        assertEquals(10, context.validScripts().size());
        assertTrue(context.errors().isEmpty());

        // when
        ExecutionPlan plan = executionPlanner.createPlan(context.validScripts());

        // then
        assertNotNull(plan);
        assertEquals(4, plan.waves().size());
        assertEquals(3, plan.waves().get(0).scripts().size());
        assertEquals(3, plan.waves().get(1).scripts().size());
        assertEquals(3, plan.waves().get(2).scripts().size());
        assertEquals(1, plan.waves().get(3).scripts().size());

        // when
        afterValidationChain.startChain(context, plan);

        // then
        assertTrue(context.errors().isEmpty());
    }
}