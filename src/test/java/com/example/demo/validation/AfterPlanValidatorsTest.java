package com.example.demo.validation;

import org.example.config.PlannerConfig;
import org.example.domain.VulnerabilityScript;
import org.example.execution.plan.ExecutionPlan;
import org.example.validation.ValidationContext;
import org.example.validation.chain.AfterPlanValidationChain;
import org.example.validation.stage.after.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {
    AfterPlanValidationChain.class,
    ScriptAppearsOnceValidator.class,
    DependencyOrderValidator.class,
    MinimalWavesValidator.class,
    ParallelismLimitValidator.class
})
public class AfterPlanValidatorsTest {

    @Autowired
    private AfterPlanValidationChain validationChain;

    @MockitoBean
    private PlannerConfig plannerConfig;

    private ValidationContext context;

    @BeforeEach
    void setUp() {
        context = new ValidationContext(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        when(plannerConfig.maxParallelExecutions()).thenReturn(2);
    }

    private VulnerabilityScript createScript(int id, Integer... deps) {
        return VulnerabilityScript.builder().scriptId(id).dependencies(List.of(deps)).build();
    }

    @Test
    void shouldCatchScriptAppearingMultipleTimes() {
        VulnerabilityScript s1 = createScript(1);
        ExecutionPlan plan = new ExecutionPlan(List.of(
            new ExecutionPlan.ExecutionWave(List.of(s1)),
            new ExecutionPlan.ExecutionWave(List.of(s1))
        ));
        context.validScripts().add(s1);

        validationChain.startChain(context, plan);

        assertEquals(3, context.errors().size());
    }

    @Test
    void shouldCatchBrokenDependencyOrder() {
        VulnerabilityScript s1 = createScript(1);
        VulnerabilityScript s2 = createScript(2, 1);

        ExecutionPlan plan = new ExecutionPlan(List.of(
            new ExecutionPlan.ExecutionWave(List.of(s2)),
            new ExecutionPlan.ExecutionWave(List.of(s1))
        ));

        context.validScripts().addAll(List.of(s1, s2));

        validationChain.startChain(context, plan);

        assertEquals(2, context.errors().size());
        assertTrue(context.errors().getFirst().contains("scheduled before its dependencies"));
    }

    @Test
    void shouldCatchExceededParallelismLimit() {
        VulnerabilityScript script1 = createScript(1);
        VulnerabilityScript script2 = createScript(2);
        VulnerabilityScript script3 = createScript(3);

        ExecutionPlan plan = new ExecutionPlan(List.of(new ExecutionPlan.ExecutionWave(List.of(script1, script2, script3))));

        context.validScripts().addAll(List.of(script1, script2, script3));

        validationChain.startChain(context, plan);

        assertEquals(1, context.errors().size());
        assertTrue(context.errors().getFirst().contains("exceeds the parallelism limit"));
    }
}