package io.github.cjlab.agent.core.dag;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class DagExecutionContext {

    private final String conversationId;
    private final String userMessage;
    private final Map<String, Object> results = new ConcurrentHashMap<>();

    public DagExecutionContext(String conversationId, String userMessage) {
        this.conversationId = conversationId;
        this.userMessage = userMessage;
    }

    public String conversationId() {
        return conversationId;
    }

    public String userMessage() {
        return userMessage;
    }

    public void putResult(String nodeId, Object result) {
        results.put(nodeId, result);
    }

    public Optional<Object> result(String nodeId) {
        return Optional.ofNullable(results.get(nodeId));
    }

    public Map<String, Object> results() {
        return Map.copyOf(results);
    }
}
