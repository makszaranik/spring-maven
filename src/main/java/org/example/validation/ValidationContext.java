package org.example.validation;

import lombok.Builder;
import org.example.domain.VulnerabilityScript;

import java.util.List;

@Builder
public record ValidationContext(
    List<VulnerabilityScript> validScripts,
    List<String> warnings,
    List<String> errors
) {
    public void addWarningLog(String message) {
        this.warnings.add(message);
    }

    public void addErrorLog(String message) {
        this.errors.add(message);
    }

}
