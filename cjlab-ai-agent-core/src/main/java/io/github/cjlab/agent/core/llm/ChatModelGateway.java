package io.github.cjlab.agent.core.llm;

import java.util.function.Consumer;

public interface ChatModelGateway {

    String chat(String message);

    default String streamChat(String message, Consumer<String> chunkConsumer) {
        String content = chat(message);
        if (chunkConsumer != null && content != null && !content.isEmpty()) {
            chunkConsumer.accept(content);
        }
        return content;
    }
}
