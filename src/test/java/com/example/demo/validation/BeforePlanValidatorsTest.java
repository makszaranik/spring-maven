package com.example.demo.validation;

import org.example.domain.VulnerabilityScript;
import org.example.validation.ValidationContext;
import org.example.validation.chain.BeforePlanValidationChain;
import org.example.validation.stage.before.CircularDependencyValidator;
import org.example.validation.stage.before.DuplicateDependencyValidator;
import org.example.validation.stage.before.MissingDependencyValidator;
import org.example.validation.stage.before.SelfDependencyValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = {
    BeforePlanValidationChain.class,
    DuplicateDependencyValidator.class,
    SelfDependencyValidator.class,
    MissingDependencyValidator.class,
    CircularDependencyValidator.class
})
public class BeforePlanValidatorsTest {

    @Autowired
    private BeforePlanValidationChain validationChain;

    private ValidationContext context;

    private VulnerabilityScript createScript(int id, Integer... deps) {
        return VulnerabilityScript.builder()
            .scriptId(id)
            .dependencies(new ArrayList<>(Arrays.asList(deps)))
            .estimatedDurationSeconds(10)
            .priority(VulnerabilityScript.Priority.MEDIUM)
            .build();
    }

    @Test
    void shouldPassWhenDataIsCorrect() {
        List<VulnerabilityScript> scripts = new ArrayList<>(List.of(
            createScript(1),
            createScript(2, 1)
        ));

        context = new ValidationContext(scripts, new ArrayList<>(), new ArrayList<>());
        validationChain.startChain(context);

        assertTrue(context.errors().isEmpty());
        assertTrue(context.warnings().isEmpty());
        assertEquals(2, context.validScripts().size());
    }

    @Test
    void shouldRemoveCycleAndTransitiveDependenciesButKeepValidScripts() {
        List<VulnerabilityScript> scripts = new ArrayList<>(List.of(
            createScript(1, 2),
            createScript(2, 1),
            createScript(3, 1),
            createScript(4)
        ));

        context = new ValidationContext(scripts, new ArrayList<>(), new ArrayList<>());
        validationChain.startChain(context);

        assertEquals(1, context.errors().size());
        assertEquals(2, context.validScripts().size());
        assertEquals(3, context.validScripts().getFirst().getScriptId());
    }

    @Test
    void shouldRemoveMissingAndTransitiveDependenciesButKeepValidScripts() {
        List<VulnerabilityScript> scripts = new ArrayList<>(List.of(
            createScript(1, 99),
            createScript(2, 1),
            createScript(3)
        ));

        context = new ValidationContext(scripts, new ArrayList<>(), new ArrayList<>());
        validationChain.startChain(context);

        assertEquals(2, context.errors().size());
        assertEquals(1, context.validScripts().size());
        assertEquals(3, context.validScripts().getFirst().getScriptId());
    }

    @Test
    void shouldDowngradeSelfDependencyToWarningAndFixIt() {
        List<VulnerabilityScript> scripts = new ArrayList<>(List.of(
            createScript(1, 1)
        ));

        context = new ValidationContext(scripts, new ArrayList<>(), new ArrayList<>());
        validationChain.startChain(context);

        assertTrue(context.errors().isEmpty());
        assertEquals(1, context.warnings().size());
        assertEquals(1, context.validScripts().size());
        assertTrue(context.validScripts().getFirst().getDependencies().isEmpty());
    }

    @Test
    void shouldAddWarningWhenDuplicateDependencyAndFixIt() {
        List<VulnerabilityScript> scripts = new ArrayList<>(List.of(
            createScript(1),
            createScript(2, 1, 1)
        ));

        context = new ValidationContext(scripts, new ArrayList<>(), new ArrayList<>());
        validationChain.startChain(context);

        assertEquals(1, context.warnings().size());
        assertEquals(2, context.validScripts().size());
        assertEquals(1, context.validScripts().get(1).getDependencies().size());
    }
}