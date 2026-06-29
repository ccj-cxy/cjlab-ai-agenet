package io.github.cjlab.agent.infrastructure.agentscope;

import io.agentscope.core.message.Msg;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.core.model.ChatUsage;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.ToolSchema;
import io.github.cjlab.agent.core.llm.ChatModelGateway;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class AgentScopeChatModelAdapter implements Model {

    private final ChatModelGateway chatModelGateway;
    private final String modelName;

    public AgentScopeChatModelAdapter(ChatModelGateway chatModelGateway, String modelName) {
        this.chatModelGateway = chatModelGateway;
        this.modelName = modelName;
    }

    @Override
    public Flux<ChatResponse> stream(List<Msg> messages, List<ToolSchema> toolSchemas, GenerateOptions options) {
        String prompt = toPrompt(messages, toolSchemas);
        String content = chatModelGateway.chat(prompt);
        ChatResponse response = ChatResponse.builder()
                .id(UUID.randomUUID().toString())
                .content(List.of(TextBlock.builder().text(content).build()))
                .usage(ChatUsage.builder().inputTokens(0).outputTokens(0).time(0).build())
                .metadata(Map.of("provider", "cjlab-chat-model-gateway"))
                .finishReason("stop")
                .build();
        return Flux.just(response);
    }

    @Override
    public String getModelName() {
        return modelName;
    }

    private String toPrompt(List<Msg> messages, List<ToolSchema> toolSchemas) {
        String conversation = messages.stream()
                .map(message -> message.getRole() + ": " + message.getTextContent())
                .collect(Collectors.joining("\n"));
        String tools = toolSchemas == null || toolSchemas.isEmpty()
                ? "None"
                : toolSchemas.stream()
                .map(tool -> "- " + tool.getName() + ": " + tool.getDescription())
                .collect(Collectors.joining("\n"));
        return """
                You are CJLab AgentScope Runtime.
                Answer the user directly. If tool information is relevant, use it in the answer.

                Available tools:
                %s

                Messages:
                %s
                """.formatted(tools, conversation);
    }
}
