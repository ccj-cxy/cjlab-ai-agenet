package io.github.cjlab.agent.tool;

import java.time.OffsetDateTime;

public class CurrentTimeTool implements AgentTool {

    @Override
    public String name() {
        return "current_time";
    }

    @Override
    public String description() {
        return "Get current server time in ISO-8601 format.";
    }

    @Override
    public ToolResult execute(ToolRequest request) {
        return new ToolResult(name(), OffsetDateTime.now().toString());
    }
}
