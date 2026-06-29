package io.github.cjlab.agent.tool;

import java.util.Map;

public record ToolRequest(
        String conversationId,
        String input,
        Map<String, Object> arguments
) {
}
