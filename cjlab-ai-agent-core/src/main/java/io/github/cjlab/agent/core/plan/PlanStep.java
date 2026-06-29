package io.github.cjlab.agent.core.plan;

import java.util.Map;
import java.util.Set;

public record PlanStep(
        String id,
        PlanStepType type,
        Set<String> dependsOn,
        Map<String, Object> parameters
) {
}
