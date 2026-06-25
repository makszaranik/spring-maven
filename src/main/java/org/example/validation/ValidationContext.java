package org.example.validation;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.domain.VulnerabilityScript;

import java.util.ArrayList;
import java.util.List;

@Data
@RequiredArgsConstructor
public class ValidationContext {

    private final List<VulnerabilityScript> scripts;
    private final List<String> warnings = new ArrayList<>();
    private final List<String> errors = new ArrayList<>();

    private List<List<Integer>> cycles = new ArrayList<>();
    private List<Integer> criticalPathNodes = new ArrayList<>();

    private int criticalPathDuration = 0;
    private double criticalPathPercentage = 0.0;

    private boolean canProceed = true;

    public void addWarningLog(String message) {
        this.warnings.add(message);
    }

    public void addErrorLog(String message) {
        this.errors.add(message);
    }

}
