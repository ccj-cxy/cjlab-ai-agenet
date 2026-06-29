package io.github.cjlab.agent.server.api;

import io.github.cjlab.agent.common.AgentException;
import io.github.cjlab.agent.server.security.CurrentUser;
import io.github.cjlab.agent.server.security.CurrentUserContext;
import io.github.cjlab.agent.server.security.UserConversationIds;
import io.github.cjlab.agent.tool.AgentTool;
import io.github.cjlab.agent.tool.ToolCallRecord;
import io.github.cjlab.agent.tool.ToolCallRecordRepository;
import io.github.cjlab.agent.tool.ToolRegistry;
import io.github.cjlab.agent.tool.ToolRequest;
import io.github.cjlab.agent.tool.ToolResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tools")
public class ToolController {

    private final ToolRegistry toolRegistry;
    private final ToolCallRecordRepository toolCallRecordRepository;

    public ToolController(ToolRegistry toolRegistry, ToolCallRecordRepository toolCallRecordRepository) {
        this.toolRegistry = toolRegistry;
        this.toolCallRecordRepository = toolCallRecordRepository;
    }

    @GetMapping
    public List<ToolDescriptor> list() {
        return toolRegistry.list().stream()
                .map(tool -> new ToolDescriptor(tool.name(), tool.description()))
                .toList();
    }

    @PostMapping("/{name}/execute")
    public ToolResult execute(
            @PathVariable String name,
            @RequestBody ToolRequest request
    ) {
        AgentTool tool = toolRegistry.findByName(name)
                .orElseThrow(() -> new AgentException("Tool not found: " + name));
        CurrentUser user = CurrentUserContext.required();
        String externalConversationId = UserConversationIds.external(request == null ? null : request.conversationId());
        return tool.execute(new ToolRequest(
                UserConversationIds.internal(user.id(), externalConversationId),
                request == null ? null : request.input(),
                request == null ? null : request.arguments()
        ));
    }

    @GetMapping("/calls")
    public List<ToolCallRecord> recentCalls(
            @RequestParam String conversationId,
            @RequestParam(defaultValue = "20") int limit
    ) {
        CurrentUser user = CurrentUserContext.required();
        String externalConversationId = UserConversationIds.external(conversationId);
        return toolCallRecordRepository.recent(UserConversationIds.internal(user.id(), externalConversationId), limit)
                .stream()
                .map(record -> new ToolCallRecord(
                        record.id(),
                        externalConversationId,
                        record.toolName(),
                        record.input(),
                        record.arguments(),
                        record.output(),
                        record.success(),
                        record.errorMessage(),
                        record.createdAt()
                ))
                .toList();
    }
    public record ToolDescriptor(String name, String description) {
    }
}
