package io.github.cjlab.agent.infrastructure.agentscope;

import io.github.cjlab.agent.tool.AgentTool;
import io.github.cjlab.agent.tool.ToolRegistry;

import java.util.List;

public class AgentScopeToolAdapter {

    private final ToolRegistry toolRegistry;

    public AgentScopeToolAdapter(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    public List<AgentTool> tools() {
        return toolRegistry.list();
    }
}
