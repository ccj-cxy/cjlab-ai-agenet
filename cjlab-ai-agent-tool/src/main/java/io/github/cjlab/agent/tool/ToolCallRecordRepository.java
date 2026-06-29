package io.github.cjlab.agent.tool;

import java.util.List;

public interface ToolCallRecordRepository {

    ToolCallRecord save(ToolCallRecord record);

    List<ToolCallRecord> recent(String conversationId, int limit);
}
