package io.github.cjlab.agent.core.runtime;

import java.util.function.Consumer;

public interface AgentRuntime {

    AgentRunResult run(AgentRunRequest request);

    default AgentRunResult stream(AgentRunRequest request, Consumer<String> chunkConsumer) {
        AgentRunResult result = run(request);
        if (chunkConsumer != null && result.content() != null && !result.content().isEmpty()) {
            chunkConsumer.accept(result.content());
        }
        return result;
    }
}
