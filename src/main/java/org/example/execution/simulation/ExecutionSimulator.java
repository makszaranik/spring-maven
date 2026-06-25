package org.example.execution.simulation;

import org.example.domain.VulnerabilityScript;
import org.example.execution.plan.ExecutionPlan;

import java.util.Collection;

public interface ExecutionSimulator {
    SimulationReport simulate(ExecutionPlan plan, Collection<VulnerabilityScript> scripts);
}
