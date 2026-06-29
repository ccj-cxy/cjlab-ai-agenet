package io.github.cjlab.agent.tool;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public class PersistentToolRegistry implements ToolRegistry {

    private final ToolRegistry delegate;
    private final ToolCallRecordRepository toolCallRecordRepository;

    public PersistentToolRegistry(ToolRegistry delegate, ToolCallRecordRepository toolCallRecordRepository) {
        this.delegate = delegate;
        this.toolCallRecordRepository = toolCallRecordRepository;
    }

    @Override
    public List<AgentTool> list() {
        return delegate.list().stream()
                .map(this::persistent)
                .toList();
    }

    @Override
    public Optional<AgentTool> findByName(String name) {
        return delegate.findByName(name)
                .map(this::persistent);
    }

    private AgentTool persistent(AgentTool tool) {
        return new AgentTool() {
            @Override
            public String name() {
                return tool.name();
            }

            @Override
            public String description() {
                return tool.description();
            }

            @Override
            public ToolResult execute(ToolRequest request) {
                try {
                    ToolResult result = tool.execute(request);
                    saveRecord(request, result, true, null);
                    return result;
                } catch (RuntimeException exception) {
                    saveRecord(request, new ToolResult(tool.name(), null), false, exception.getMessage());
                    throw exception;
                }
            }
        };
    }

    private void saveRecord(ToolRequest request, ToolResult result, boolean success, String errorMessage) {
        toolCallRecordRepository.save(new ToolCallRecord(
                null,
                request == null ? null : request.conversationId(),
                result.toolName(),
                request == null ? null : request.input(),
                request == null ? null : request.arguments(),
                result.content(),
                success,
                errorMessage,
                Instant.now()
        ));
    }
}
