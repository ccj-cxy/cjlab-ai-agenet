package io.github.cjlab.agent.core.plan;

import java.util.List;

public record AgentPlan(
        String goal,
        List<PlanStep> steps
) {
}
