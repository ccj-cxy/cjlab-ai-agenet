package io.github.cjlab.agent.tool;

public interface AgentTool {

    String name();

    String description();

    ToolResult execute(ToolRequest request);
}
