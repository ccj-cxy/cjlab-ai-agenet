package io.github.cjlab.agent.core.runtime;

public record  AgentRunRequest(
        String conversationId,
        String message
) {
}
