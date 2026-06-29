package io.github.cjlab.agent.tool;

import java.time.Instant;
import java.util.Map;

public record ToolCallRecord(
        String id,
        String conversationId,
        String toolName,
        String input,
        Map<String, Object> arguments,
        String output,
        boolean success,
        String errorMessage,
        Instant createdAt
) {
}
