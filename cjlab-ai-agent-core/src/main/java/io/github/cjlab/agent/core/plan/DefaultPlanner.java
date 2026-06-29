package io.github.cjlab.agent.core.plan;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class DefaultPlanner implements Planner {

    @Override
    public AgentPlan plan(PlanningContext context) {
        return new AgentPlan(context.userMessage(), List.of(
                new PlanStep("memory", PlanStepType.MEMORY, Set.of(), Map.of("limit", 8)),
                new PlanStep("retrieval", PlanStepType.RETRIEVAL, Set.of(), Map.of("limit", 3)),
                new PlanStep("tool", PlanStepType.TOOL, Set.of(), Map.of()),
                new PlanStep("generation", PlanStepType.GENERATION, Set.of("memory", "retrieval", "tool"), Map.of())
        ));
    }
}
