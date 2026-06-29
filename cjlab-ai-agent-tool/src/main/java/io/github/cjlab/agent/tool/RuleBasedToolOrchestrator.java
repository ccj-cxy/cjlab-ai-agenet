package io.github.cjlab.agent.tool;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RuleBasedToolOrchestrator implements ToolOrchestrator {

    private final ToolRegistry toolRegistry;

    public RuleBasedToolOrchestrator(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    @Override
    public List<ToolResult> execute(String conversationId, String userMessage) {
        if (userMessage == null) {
            return List.of();
        }
        String normalized = userMessage.toLowerCase(Locale.ROOT);
        if (normalized.contains("time") || normalized.contains("时间") || normalized.contains("now")) {
            return toolRegistry.findByName("current_time")
                    .map(tool -> List.of(tool.execute(new ToolRequest(conversationId, userMessage, Map.of()))))
                    .orElseGet(List::of);
        }
        return List.of();
    }
}
