package io.github.cjlab.agent.tool;

import java.util.List;
import java.util.Optional;

public interface ToolRegistry {

    List<AgentTool> list();

    Optional<AgentTool> findByName(String name);
}
