package io.github.cjlab.agent.tool;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

public class InMemoryToolRegistry implements ToolRegistry {

    private final List<AgentTool> tools = new CopyOnWriteArrayList<>();

    public InMemoryToolRegistry(List<AgentTool> tools) {
        this.tools.addAll(tools);
    }

    @Override
    public List<AgentTool> list() {
        return List.copyOf(tools);
    }

    @Override
    public Optional<AgentTool> findByName(String name) {
        return tools.stream()
                .filter(tool -> tool.name().equals(name))
                .findFirst();
    }
}
