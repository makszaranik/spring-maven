package org.example.controller;


import lombok.RequiredArgsConstructor;
import org.example.domain.VulnerabilityScript;
import org.example.execution.executor.ScriptExecutor;
import org.example.execution.plan.ExecutionPlan;
import org.example.execution.plan.ExecutionPlanner;
import org.example.execution.plan.PlanAnalysis;
import org.example.execution.simulation.SimulationReport;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.util.List;

@ShellComponent
@RequiredArgsConstructor
public class ScriptsExecutorController {

    private final ScriptExecutor scriptExecutor;
    private final ObjectMapper objectMapper;
    private final ExecutionPlanner executionPlanner;

    @ShellMethod(key = "executor run")
    public String execute(@ShellOption(value = "--source") String scriptsSource) {
        File scriptsFile = new File(scriptsSource);

        if (!scriptsFile.exists()) {
            return "File with scripts not found: " + scriptsSource;
        }

        try {
            List<VulnerabilityScript> scripts = objectMapper.readValue(scriptsFile, new TypeReference<>() {});

            if (scripts.isEmpty()) {
                return "File is empty or contains no scripts.";
            }

            ExecutionPlan executionPlan = executionPlanner.createPlan(scripts);
            scriptExecutor.validate(executionPlan);
            SimulationReport result = scriptExecutor.executeScripts(scripts, executionPlan);
            PlanAnalysis planAnalysis = scriptExecutor.analyze(executionPlan);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(planAnalysis);

        } catch (Exception e) {
            return "Error executing scripts: " + e.getMessage();
        }
    }
}
