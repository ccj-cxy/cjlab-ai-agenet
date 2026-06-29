package io.github.cjlab.agent.infrastructure.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.cjlab.agent.common.AgentException;
import io.github.cjlab.agent.core.llm.ChatModelGateway;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class OpenAiCompatibleChatModelGateway implements ChatModelGateway {

    private final Properties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public OpenAiCompatibleChatModelGateway(Properties properties) {
        this.properties = properties;
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.timeout())
                .build();
    }

    @Override
    public String chat(String message) {
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw new AgentException("CJLAB_OPENAI_API_KEY must be configured for openai-compatible profile.");
        }

        Map<String, Object> payload = basePayload(message, false);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint()))
                    .timeout(properties.timeout())
                    .header("Authorization", "Bearer " + properties.apiKey())
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new AgentException("OpenAI compatible relay returned HTTP "
                        + response.statusCode() + ": " + response.body());
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode messageNode = root.path("choices").path(0).path("message");
            JsonNode reasoningContent = messageNode.path("reasoning_content");
            JsonNode content = messageNode.path("content");
            if (!content.isMissingNode() && !content.isNull()) {
                return withReasoning(reasoningContent, content.asText());
            }

            JsonNode text = root.path("choices").path(0).path("text");
            if (!text.isMissingNode() && !text.isNull()) {
                return withReasoning(reasoningContent, text.asText());
            }

            throw new AgentException("OpenAI compatible relay response does not contain choices[0].message.content.");
        } catch (IOException exception) {
            throw new AgentException("OpenAI compatible relay request failed.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AgentException("OpenAI compatible relay request was interrupted.", exception);
        }
    }

    @Override
    public String streamChat(String message, Consumer<String> chunkConsumer) {
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw new AgentException("CJLAB_OPENAI_API_KEY must be configured for openai-compatible profile.");
        }

        Map<String, Object> payload = basePayload(message, true);
        StringBuilder content = new StringBuilder();
        boolean[] reasoningOpen = {false};

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint()))
                    .timeout(properties.timeout())
                    .header("Authorization", "Bearer " + properties.apiKey())
                    .header("Content-Type", "application/json")
                    .header("Accept", "text/event-stream")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();

            HttpResponse<Stream<String>> response = httpClient.send(request, HttpResponse.BodyHandlers.ofLines());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new AgentException("OpenAI compatible relay returned HTTP "
                        + response.statusCode());
            }

            try (Stream<String> lines = response.body()) {
                lines.forEach(line -> {
                    if (!line.startsWith("data:")) {
                        return;
                    }
                    String data = line.substring("data:".length()).trim();
                    if (data.isBlank() || "[DONE]".equals(data)) {
                        return;
                    }
                    StreamChunk chunk = readStreamChunk(data);
                    if (!chunk.reasoning().isEmpty() && !reasoningOpen[0]) {
                        appendStreamChunk(content, chunkConsumer, "<think>");
                        reasoningOpen[0] = true;
                    }
                    if (!chunk.reasoning().isEmpty()) {
                        appendStreamChunk(content, chunkConsumer, chunk.reasoning());
                    }
                    if (!chunk.content().isEmpty() && reasoningOpen[0]) {
                        appendStreamChunk(content, chunkConsumer, "</think>\n");
                        reasoningOpen[0] = false;
                    }
                    if (!chunk.content().isEmpty()) {
                        appendStreamChunk(content, chunkConsumer, chunk.content());
                    }
                });
            }
            if (reasoningOpen[0]) {
                appendStreamChunk(content, chunkConsumer, "</think>\n");
            }
            return content.toString();
        } catch (IOException exception) {
            throw new AgentException("OpenAI compatible relay stream request failed.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AgentException("OpenAI compatible relay stream request was interrupted.", exception);
        }
    }

    private Map<String, Object> basePayload(String message, boolean stream) {
        return Map.of(
                "model", properties.model(),
                "temperature", properties.temperature(),
                "stream", stream,
                "messages", List.of(Map.of(
                        "role", "user",
                        "content", message
                ))
        );
    }

    private StreamChunk readStreamChunk(String data) {
        try {
            JsonNode root = objectMapper.readTree(data);
            JsonNode delta = root.path("choices").path(0).path("delta");
            JsonNode reasoningContent = delta.path("reasoning_content");
            JsonNode content = delta.path("content");
            String reasoning = "";
            if (!reasoningContent.isMissingNode() && !reasoningContent.isNull()) {
                reasoning = reasoningContent.asText();
            }
            if (!content.isMissingNode() && !content.isNull()) {
                return new StreamChunk(reasoning, content.asText());
            }
            JsonNode text = root.path("choices").path(0).path("text");
            if (!text.isMissingNode() && !text.isNull()) {
                return new StreamChunk(reasoning, text.asText());
            }
            return new StreamChunk(reasoning, "");
        } catch (IOException exception) {
            throw new AgentException("Failed to parse OpenAI compatible stream chunk.", exception);
        }
    }

    private String withReasoning(JsonNode reasoningContent, String content) {
        if (reasoningContent == null || reasoningContent.isMissingNode() || reasoningContent.isNull()) {
            return content;
        }
        String reasoning = reasoningContent.asText();
        if (reasoning.isBlank()) {
            return content;
        }
        return "<think>" + reasoning + "</think>\n" + content;
    }

    private void appendStreamChunk(StringBuilder content, Consumer<String> chunkConsumer, String chunk) {
        content.append(chunk);
        if (chunkConsumer != null) {
            chunkConsumer.accept(chunk);
        }
    }

    private record StreamChunk(String reasoning, String content) {
    }

    private String endpoint() {
        String baseUrl = trimRight(properties.baseUrl(), "/");
        String path = properties.completionsPath().startsWith("/")
                ? properties.completionsPath()
                : "/" + properties.completionsPath();
        return baseUrl + path;
    }

    private static String trimRight(String value, String suffix) {
        String result = value;
        while (result.endsWith(suffix)) {
            result = result.substring(0, result.length() - suffix.length());
        }
        return result;
    }

    @ConfigurationProperties(prefix = "cjlab.openai-compatible")
    public record Properties(
            String baseUrl,
            String apiKey,
            String model,
            String completionsPath,
            double temperature,
            Duration timeout
    ) {
        public Properties {
            baseUrl = (baseUrl == null || baseUrl.isBlank()) ? "https://api.openai.com" : baseUrl;
            model = (model == null || model.isBlank()) ? "gpt-4o-mini" : model;
            completionsPath = (completionsPath == null || completionsPath.isBlank())
                    ? "/v1/chat/completions"
                    : completionsPath;
            timeout = timeout == null ? Duration.ofSeconds(60) : timeout;
        }
    }
}
