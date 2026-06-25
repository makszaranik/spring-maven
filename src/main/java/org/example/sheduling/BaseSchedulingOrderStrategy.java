package org.example.sheduling;

import org.example.domain.VulnerabilityScript;
import org.springframework.stereotype.Component;

import java.util.Comparator;

@Component
public class BaseSchedulingOrderStrategy implements SchedulingOrderStrategy {

    @Override
    public int compare(VulnerabilityScript o1, VulnerabilityScript o2) {
        return Comparator.comparing(VulnerabilityScript::getPriority, Comparator.reverseOrder())
            .thenComparing(VulnerabilityScript::getEstimatedDurationSeconds, Comparator.reverseOrder())
            .thenComparing(VulnerabilityScript::getScriptId)
            .compare(o1, o2);
    }
}
