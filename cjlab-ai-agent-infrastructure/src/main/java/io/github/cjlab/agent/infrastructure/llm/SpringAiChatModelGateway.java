package io.github.cjlab.agent.infrastructure.llm;

import io.github.cjlab.agent.core.llm.ChatModelGateway;
import org.springframework.ai.chat.client.ChatClient;

public class SpringAiChatModelGateway implements ChatModelGateway {

    private final ChatClient chatClient;

    public SpringAiChatModelGateway(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @Override
    public String chat(String message) {
        return chatClient.prompt()
                .user(message)
                .call()
                .content();
    }
}
