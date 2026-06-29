package org.example;

import lombok.extern.slf4j.Slf4j;
import org.example.domain.VulnerabilityScript;
import org.example.execution.executor.ScriptExecutor;
import org.example.execution.plan.ExecutionPlan;
import org.example.execution.plan.ExecutionPlanner;
import org.example.execution.plan.PlanAnalysis;
import org.example.validation.ValidationResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.ConfigurableApplicationContext;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@SpringBootApplication
@ConfigurationPropertiesScan
public class DemoApplication {


	public static void main(String[] args) {
		ConfigurableApplicationContext context = SpringApplication.run(DemoApplication.class, args);

		ObjectMapper objectMapper = context.getBean(ObjectMapper.class);
		ScriptExecutor scriptExecutor = context.getBean(ScriptExecutor.class);
		ExecutionPlanner executionPlanner = context.getBean(ExecutionPlanner.class);

		File scriptsFile = new File("src/main/resources/scripts.json");
		List<VulnerabilityScript> scripts = objectMapper.readValue(scriptsFile, new TypeReference<>() {});

		ValidationResult validationResult = scriptExecutor.validate(scripts);
		ExecutionPlan executionPlan = validationResult.executionPlan();

		/*
		VulnerabilityScript script = VulnerabilityScript.builder()
			.scriptId(6)
			.dependencies(List.of(1, 5))
			.priority(VulnerabilityScript.Priority.MEDIUM)
			.build();

		executionPlanner.addScript(executionPlan, script);
		 */

		scriptExecutor.executeScripts(executionPlan);
		PlanAnalysis planAnalysis = scriptExecutor.analyze(validationResult.validScripts(), executionPlan);

		log.info("waves: {}", executionPlan.waves());
		log.info("plan {}", planAnalysis);

	}

}
