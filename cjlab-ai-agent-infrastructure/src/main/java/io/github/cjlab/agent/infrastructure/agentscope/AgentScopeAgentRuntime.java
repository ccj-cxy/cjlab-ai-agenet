package io.github.cjlab.agent.infrastructure.agentscope;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.tool.Toolkit;
import io.github.cjlab.agent.common.AgentException;
import io.github.cjlab.agent.core.llm.ChatModelGateway;
import io.github.cjlab.agent.core.runtime.AgentRunRequest;
import io.github.cjlab.agent.core.runtime.AgentRunResult;
import io.github.cjlab.agent.core.runtime.AgentRuntime;
import io.github.cjlab.agent.memory.ConversationMemory;
import io.github.cjlab.agent.memory.ConversationMessage;
import io.github.cjlab.agent.memory.MessageRole;
import io.github.cjlab.agent.rag.KnowledgeRetriever;
import io.github.cjlab.agent.tool.ToolRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AgentScopeAgentRuntime implements AgentRuntime {

    private final ChatModelGateway chatModelGateway;
    private final ConversationMemory conversationMemory;
    private final KnowledgeRetriever knowledgeRetriever;
    private final ToolRegistry toolRegistry;

    public AgentScopeAgentRuntime(
            ChatModelGateway chatModelGateway,
            ConversationMemory conversationMemory,
            KnowledgeRetriever knowledgeRetriever,
            ToolRegistry toolRegistry
    ) {
        this.chatModelGateway = chatModelGateway;
        this.conversationMemory = conversationMemory;
        this.knowledgeRetriever = knowledgeRetriever;
        this.toolRegistry = toolRegistry;
    }

    @Override
    public AgentRunResult run(AgentRunRequest request) {
        try {
            Toolkit toolkit = new Toolkit();
            toolkit.registerTool(new AgentScopeRagTool(knowledgeRetriever));

            ReActAgent agent = ReActAgent.builder()
                    .name("cjlab-agentscope-agent")
                    .description("CJLab AgentScope runtime agent")
                    .sysPrompt(systemPrompt())
                    .model(new AgentScopeChatModelAdapter(chatModelGateway, "cjlab-chat-model-gateway"))
                    .toolkit(toolkit)
                    .memory(new InMemoryMemory())
                    .maxIters(3)
                    .build();

            Msg response = agent.call(messages(request)).block();
            if (response == null) {
                throw new AgentException("AgentScope runtime returned empty response.");
            }
            return new AgentRunResult(request.conversationId(), response.getTextContent(), Map.of(
                    "runtime", "agentscope",
                    "tools", toolRegistry.list().stream().map(tool -> tool.name()).toList()
            ));
        } catch (AgentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new AgentException("AgentScope runtime execution failed.", exception);
        }
    }

    private List<Msg> messages(AgentRunRequest request) {
        List<Msg> messages = new ArrayList<>();
        for (ConversationMessage message : conversationMemory.recent(request.conversationId(), 8)) {
            messages.add(Msg.builder()
                    .name(roleName(message.role()))
                    .role(toAgentScopeRole(message.role()))
                    .textContent(message.content())
                    .build());
        }
        messages.add(Msg.builder()
                .name("user")
                .role(MsgRole.USER)
                .textContent(request.message())
                .build());
        return messages;
    }

    private MsgRole toAgentScopeRole(MessageRole role) {
        return switch (role) {
            case USER -> MsgRole.USER;
            case ASSISTANT -> MsgRole.ASSISTANT;
            case TOOL -> MsgRole.TOOL;
        };
    }

    private String roleName(MessageRole role) {
        return role.name().toLowerCase();
    }

    private String systemPrompt() {
        return """
                You are CJLab AgentScope Runtime.
                You can use tools when needed.
                Prefer concise, actionable answers.
                When project knowledge is needed, use rag_search.
                """;
    }
}
