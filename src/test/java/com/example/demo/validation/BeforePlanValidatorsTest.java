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
    CircularDependencyValidator.class,
    DuplicateDependencyValidator.class,
    MissingDependencyValidator.class,
    SelfDependencyValidator.class
})
public class BeforePlanValidatorsTest {

    @Autowired
    private BeforePlanValidationChain validationChain;

    private ValidationContext context;

    @BeforeEach
    void setUp() {
        context = ValidationContext.builder()
            .warnings(new ArrayList<>())
            .errors(new ArrayList<>())
            .build();
    }

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

        //given
        List<VulnerabilityScript> scripts = List.of(
            createScript(1),
            createScript(2, 1)
        );

        //when
        context = new ValidationContext(scripts, new ArrayList<>(), new ArrayList<>());
        validationChain.startChain(context);

        //then
        assertTrue(context.isCanProceed());
        assertTrue(context.errors().isEmpty());
        assertTrue(context.warnings().isEmpty());
    }

    @Test
    void shouldAddErrorWhenCycleDetected() {

        //given
        List<VulnerabilityScript> scripts = List.of(
            createScript(1, 2),
            createScript(2, 1)
        );

        //when
        context = new ValidationContext(scripts, new ArrayList<>(), new ArrayList<>());
        validationChain.startChain(context);

        //then
        assertFalse(context.isCanProceed());
        assertEquals(1, context.errors().size());
        assertTrue(context.errors().getFirst().contains("The execution graph contains an unresolvable cycle:"));
    }

    @Test
    void shouldAddErrorWhenDependencyIsMissing() {

        //given
        List<VulnerabilityScript> scripts = List.of(
            createScript(1, 99)
        );

        //when
        context = new ValidationContext(scripts, new ArrayList<>(), new ArrayList<>());
        validationChain.startChain(context);

        //then
        assertFalse(context.isCanProceed());
        assertTrue(context.errors().getFirst().contains("contains missing dependency: 99"));
    }

    @Test
    void shouldAddWarningWhenDuplicateDependency() {

        //given
        List<VulnerabilityScript> scripts = List.of(
            createScript(2),
            createScript(1, 2, 2)
        );

        //when
        context = new ValidationContext(scripts, new ArrayList<>(), new ArrayList<>());
        validationChain.startChain(context);

        //then
        assertTrue(context.isCanProceed());
        assertEquals(1, context.warnings().size());
        assertTrue(context.warnings().getFirst().contains("contains duplicated dependencies"));
        assertEquals(1, scripts.get(1).getDependencies().size(), "Duplicate should be removed");
    }


    @Test
    void shouldAddWarningWhenSelfDependency() {

        //given
        List<VulnerabilityScript> scripts = List.of(
            createScript(1, 1)
        );

        //when
        context = new ValidationContext(scripts, new ArrayList<>(), new ArrayList<>());
        validationChain.startChain(context);

        //then
        assertEquals(1, context.warnings().size());
        assertTrue(context.warnings().getFirst().contains("depends on itself"));
    }

}
