package io.github.cjlab.agent.tool;

import java.util.Map;

public record ToolInvocation(
        String toolName,
        String input,
        Map<String, Object> arguments
) {
}
