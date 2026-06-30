package com.example.demo;

import org.example.config.PlannerConfig;
import org.example.config.SimulationConfig;
import org.example.domain.VulnerabilityScript;
import org.example.execution.executor.DefaultScriptExecutor;
import org.example.execution.plan.BFSExecutionPlanner;
import org.example.execution.plan.ExecutionPlan;
import org.example.execution.plan.PlanAnalysis;
import org.example.execution.simulation.ParallelExecutionSimulator;
import org.example.execution.simulation.SimulationReport;
import org.example.sheduling.BaseSchedulingOrderStrategy;
import org.example.validation.ValidationContext;
import org.example.validation.ValidationResult;
import org.example.validation.chain.AfterPlanValidationChain;
import org.example.validation.chain.BeforePlanValidationChain;
import org.example.validation.stage.after.*;
import org.example.validation.stage.before.*;
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
    ParallelismLimitValidator.class,
    DefaultScriptExecutor.class,
    ParallelExecutionSimulator.class
})
public class PlannerPipelineE2ETest {

    @Autowired
    private DefaultScriptExecutor scriptExecutor;

    @MockitoBean
    private PlannerConfig plannerConfig;

    @MockitoBean
    private SimulationConfig simulationConfig;

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
        when(simulationConfig.failureProbability()).thenReturn(0.0);
        when(simulationConfig.maxRetries()).thenReturn(3);

        List<VulnerabilityScript> rawScripts = new ArrayList<>(List.of(
            createScript(1),
            createScript(2, 1),
            createScript(3, 1),
            createScript(4, 2, 3)
        ));

        ValidationContext initialContext = ValidationContext.builder()
            .validScripts(new ArrayList<>(rawScripts))
            .warnings(new ArrayList<>())
            .errors(new ArrayList<>())
            .build();

        // when
        ValidationResult initialResult = scriptExecutor.validate(rawScripts, initialContext);
        ExecutionPlan plan = initialResult.validExecutionPlan();
        PlanAnalysis analysis = scriptExecutor.analyze(initialResult.validScripts(), plan);

        // then
        assertTrue(initialResult.errors().isEmpty());
        assertNotNull(plan);
        assertEquals(3, plan.waves().size());
        assertEquals(4, analysis.totalScripts());
        assertEquals(3, analysis.totalWaves());
        assertEquals(2, analysis.maxParallelismAchieved());
    }

    @Test
    void shouldHandleCompletelyBrokenDataGracefully() {
        // given
        when(plannerConfig.maxParallelExecutions()).thenReturn(5);

        List<VulnerabilityScript> rawScripts = new ArrayList<>(List.of(
            createScript(1, 2),
            createScript(2, 1),
            createScript(3, 99)
        ));

        ValidationContext context = ValidationContext.builder()
            .validScripts(new ArrayList<>(rawScripts))
            .warnings(new ArrayList<>())
            .errors(new ArrayList<>())
            .build();

        // when
        ValidationResult result = scriptExecutor.validate(rawScripts, context);

        // then
        assertFalse(result.errors().isEmpty());
        assertTrue(result.validScripts().isEmpty());
        assertNotNull(result.validExecutionPlan());
        assertTrue(result.validExecutionPlan().waves().isEmpty());
    }

    @Test
    void shouldEnforceStrictParallelismLimitsOnWideGraph() {
        // given
        when(plannerConfig.maxParallelExecutions()).thenReturn(3);

        List<VulnerabilityScript> rawScripts = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            rawScripts.add(createScript(i));
        }

        ValidationContext context = ValidationContext.builder()
            .validScripts(new ArrayList<>(rawScripts))
            .warnings(new ArrayList<>())
            .errors(new ArrayList<>())
            .build();

        // when
        ValidationResult result = scriptExecutor.validate(rawScripts, context);
        ExecutionPlan plan = result.validExecutionPlan();

        // then
        assertTrue(result.errors().isEmpty());
        assertEquals(10, result.validScripts().size());
        assertNotNull(plan);
        assertEquals(4, plan.waves().size());
        assertEquals(3, plan.waves().get(0).scripts().size());
        assertEquals(3, plan.waves().get(1).scripts().size());
        assertEquals(3, plan.waves().get(2).scripts().size());
        assertEquals(1, plan.waves().get(3).scripts().size());
    }

    @Test
    void shouldSuccessfullyAddValidScriptToPlan() {
        // given
        when(plannerConfig.maxParallelExecutions()).thenReturn(2);

        List<VulnerabilityScript> initialScripts = new ArrayList<>(List.of(
            createScript(1),
            createScript(2, 1),
            createScript(3, 1),
            createScript(4, 2, 3)
        ));

        ValidationContext initialContext = ValidationContext.builder()
            .validScripts(new ArrayList<>(initialScripts))
            .warnings(new ArrayList<>())
            .errors(new ArrayList<>())
            .build();

        ValidationResult initialResult = scriptExecutor.validate(initialScripts, initialContext);
        ExecutionPlan plan = initialResult.validExecutionPlan();

        VulnerabilityScript newScript = createScript(5, 4);

        // when
        ValidationResult addResult = scriptExecutor.addAndValidateScript(plan, newScript, initialContext);

        // then
        assertTrue(addResult.errors().isEmpty());
        assertEquals(5, addResult.validScripts().size());

        int scriptsInPlanAfterAdd = addResult.validExecutionPlan().waves().stream()
            .mapToInt(w -> w.scripts().size())
            .sum();
        assertEquals(5, scriptsInPlanAfterAdd);
        assertEquals(4, addResult.validExecutionPlan().waves().size());
    }

    @Test
    void shouldRejectInvalidScriptAndKeepPlanIntact() {
        // given
        when(plannerConfig.maxParallelExecutions()).thenReturn(2);

        List<VulnerabilityScript> initialScripts = new ArrayList<>(List.of(
            createScript(1),
            createScript(2, 1)
        ));

        ValidationContext initialContext = ValidationContext.builder()
            .validScripts(new ArrayList<>(initialScripts))
            .warnings(new ArrayList<>())
            .errors(new ArrayList<>())
            .build();

        ValidationResult initialResult = scriptExecutor.validate(initialScripts, initialContext);
        ExecutionPlan plan = initialResult.validExecutionPlan();

        int initialWavesCount = plan.waves().size();

        VulnerabilityScript brokenScript = createScript(3, 3);

        // when
        ValidationResult addResult = scriptExecutor.addAndValidateScript(plan, brokenScript, initialContext);

        // then
        assertFalse(addResult.warnings().isEmpty());
        assertEquals(3, addResult.validScripts().size());

        int currentScriptsCount = addResult.validExecutionPlan().waves().stream()
            .mapToInt(w -> w.scripts().size())
            .sum();

        assertEquals(initialWavesCount, addResult.validExecutionPlan().waves().size());
        assertEquals(3, currentScriptsCount);
    }
}