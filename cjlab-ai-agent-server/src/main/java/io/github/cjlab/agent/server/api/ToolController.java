package io.github.cjlab.agent.server.api;

import io.github.cjlab.agent.common.AgentException;
import io.github.cjlab.agent.tool.AgentTool;
import io.github.cjlab.agent.tool.ToolRegistry;
import io.github.cjlab.agent.tool.ToolRequest;
import io.github.cjlab.agent.tool.ToolResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tools")
public class ToolController {

    private final ToolRegistry toolRegistry;

    public ToolController(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    @GetMapping
    public List<ToolDescriptor> list() {
        return toolRegistry.list().stream()
                .map(tool -> new ToolDescriptor(tool.name(), tool.description()))
                .toList();
    }

    @PostMapping("/{name}/execute")
    public ToolResult execute(@PathVariable String name, @RequestBody ToolRequest request) {
        AgentTool tool = toolRegistry.findByName(name)
                .orElseThrow(() -> new AgentException("Tool not found: " + name));
        return tool.execute(request);
    }

    public record ToolDescriptor(String name, String description) {
    }
}
