package io.github.cjlab.agent.core.plan;

public record PlanningContext(
        String conversationId,
        String userMessage
) {
}
