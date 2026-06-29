package io.github.cjlab.agent.core.chat;

import java.util.function.Consumer;

public interface AgentService {

    ChatResponse chat(ChatRequest request);

    ChatResponse streamChat(ChatRequest request, Consumer<String> chunkConsumer);
}
