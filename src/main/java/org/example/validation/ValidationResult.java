package org.example.validation;

import lombok.Builder;

import java.util.List;

@Builder
public record ValidationResult(
    List<String> warnings,
    List<String> errors
) {}
