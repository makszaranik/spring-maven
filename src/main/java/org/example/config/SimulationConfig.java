package org.example.config;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "simulation")
public record SimulationConfig(
    double failureProbability,
    int maxRetries
) {}
