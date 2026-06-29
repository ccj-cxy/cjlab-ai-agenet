package io.github.cjlab.agent.core.runtime;

import java.util.Map;

public record AgentRunResult(
        String conversationId,
        String content,
        Map<String, Object> metadata
) {
}
