package io.github.cjlab.agent.tool;

import java.util.List;

public interface ToolOrchestrator {

    List<ToolResult> execute(String conversationId, String userMessage);
}
