package org.example;

import lombok.extern.slf4j.Slf4j;
import org.example.domain.VulnerabilityScript;
import org.example.execution.executor.ScriptExecutor;
import org.example.execution.plan.ExecutionPlan;
import org.example.execution.plan.PlanAnalysis;
import org.example.graph.DependencyGraph;
import org.example.graph.DependencyGraphUtils;
import org.example.validation.ValidationContext;
import org.example.validation.ValidationResult;
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

		File scriptsFile = new File("src/main/resources/scripts.json");
		List<VulnerabilityScript> scripts = objectMapper.readValue(scriptsFile, new TypeReference<>() {});

		System.out.println(DependencyGraphUtils.findAllCycles(DependencyGraph.buildGraph(scripts)));

		/*
		ValidationContext validationContext = ValidationContext.builder()
			.validScripts(scripts)
			.warnings(new ArrayList<>())
			.errors(new ArrayList<>())
			.build();

		ValidationResult validationResult = scriptExecutor.validate(scripts, validationContext);
		scriptExecutor.executeScripts(validationResult.validExecutionPlan());
		PlanAnalysis planAnalysis = scriptExecutor.analyze(validationResult.validScripts(), validationResult.validExecutionPlan());

		log.info("waves: {}", validationResult.validExecutionPlan().waves());
		log.info("plan {}", planAnalysis);

		 */
	}

}
