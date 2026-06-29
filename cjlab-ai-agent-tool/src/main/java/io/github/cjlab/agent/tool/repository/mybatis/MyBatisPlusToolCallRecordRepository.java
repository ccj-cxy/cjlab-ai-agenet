package io.github.cjlab.agent.tool.repository.mybatis;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.cjlab.agent.common.AgentException;
import io.github.cjlab.agent.tool.ToolCallRecord;
import io.github.cjlab.agent.tool.ToolCallRecordRepository;
import io.github.cjlab.agent.tool.persistence.entity.ToolCallRecordEntity;
import io.github.cjlab.agent.tool.persistence.mapper.ToolCallRecordMapper;

import java.time.Instant;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MyBatisPlusToolCallRecordRepository implements ToolCallRecordRepository {

    private static final TypeReference<Map<String, Object>> ARGUMENTS_TYPE = new TypeReference<>() {
    };

    private final ToolCallRecordMapper toolCallRecordMapper;
    private final ObjectMapper objectMapper;

    public MyBatisPlusToolCallRecordRepository(ToolCallRecordMapper toolCallRecordMapper) {
        this.toolCallRecordMapper = toolCallRecordMapper;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public ToolCallRecord save(ToolCallRecord record) {
        ToolCallRecord normalized = normalize(record);
        toolCallRecordMapper.insert(toEntity(normalized));
        return normalized;
    }

    @Override
    public List<ToolCallRecord> recent(String conversationId, int limit) {
        int safeLimit = Math.max(1, limit);
        return toolCallRecordMapper.selectList(new LambdaQueryWrapper<ToolCallRecordEntity>()
                        .eq(ToolCallRecordEntity::getConversationId, conversationId)
                        .orderByDesc(ToolCallRecordEntity::getCreateTime)
                        .last("LIMIT " + safeLimit))
                .stream()
                .map(this::toDomain)
                .sorted(Comparator.comparing(ToolCallRecord::createdAt))
                .toList();
    }

    private ToolCallRecord normalize(ToolCallRecord record) {
        return new ToolCallRecord(
                record.id() == null || record.id().isBlank() ? UUID.randomUUID().toString() : record.id(),
                record.conversationId(),
                record.toolName(),
                record.input(),
                record.arguments() == null ? Map.of() : record.arguments(),
                record.output(),
                record.success(),
                record.errorMessage(),
                record.createdAt() == null ? Instant.now() : record.createdAt()
        );
    }

    private ToolCallRecordEntity toEntity(ToolCallRecord record) {
        ToolCallRecordEntity entity = new ToolCallRecordEntity();
        entity.setId(record.id());
        entity.setConversationId(record.conversationId());
        entity.setToolName(record.toolName());
        entity.setInput(record.input());
        entity.setArgumentsJson(writeArguments(record.arguments()));
        entity.setOutput(record.output());
        entity.setSuccess(record.success());
        entity.setErrorMessage(record.errorMessage());
        entity.setCreateTime(toDate(record.createdAt()));
        entity.setDeleted(false);
        return entity;
    }

    private ToolCallRecord toDomain(ToolCallRecordEntity entity) {
        return new ToolCallRecord(
                entity.getId(),
                entity.getConversationId(),
                entity.getToolName(),
                entity.getInput(),
                readArguments(entity.getArgumentsJson()),
                entity.getOutput(),
                Boolean.TRUE.equals(entity.getSuccess()),
                entity.getErrorMessage(),
                toInstant(entity.getCreateTime())
        );
    }

    private String writeArguments(Map<String, Object> arguments) {
        try {
            return objectMapper.writeValueAsString(arguments == null ? Map.of() : arguments);
        } catch (Exception exception) {
            throw new AgentException("Failed to serialize tool call arguments.", exception);
        }
    }

    private Map<String, Object> readArguments(String argumentsJson) {
        if (argumentsJson == null || argumentsJson.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(argumentsJson, ARGUMENTS_TYPE);
        } catch (Exception exception) {
            throw new AgentException("Failed to deserialize tool call arguments.", exception);
        }
    }

    private Date toDate(Instant instant) {
        return instant == null ? null : Date.from(instant);
    }

    private Instant toInstant(Date date) {
        return date == null ? null : date.toInstant();
    }
}
