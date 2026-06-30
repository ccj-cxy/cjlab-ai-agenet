package io.github.cjlab.agent.tool;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ArrayList;

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
        List<ToolResult> results = new ArrayList<>();
        String normalized = userMessage.toLowerCase(Locale.ROOT);
        if (normalized.contains("time") || normalized.contains("时间") || normalized.contains("now")) {
            toolRegistry.findByName("current_time")
                    .map(tool -> tool.execute(new ToolRequest(conversationId, userMessage, Map.of())))
                    .ifPresent(results::add);
        }
        if (needsWebSearch(normalized)) {
            toolRegistry.findByName("web_search")
                    .map(tool -> tool.execute(new ToolRequest(
                            conversationId,
                            userMessage,
                            Map.of("query", userMessage, "limit", 5)
                    )))
                    .ifPresent(results::add);
        }
        return List.copyOf(results);
    }

    private boolean needsWebSearch(String normalized) {
        return normalized.contains("搜索")
                || normalized.contains("检索")
                || normalized.contains("网页")
                || normalized.contains("网站")
                || normalized.contains("查一下")
                || normalized.contains("查找")
                || normalized.contains("最新")
                || normalized.contains("新闻")
                || normalized.contains("search")
                || normalized.contains("web")
                || normalized.contains("website")
                || normalized.contains("latest")
                || normalized.contains("news");
    }
}
