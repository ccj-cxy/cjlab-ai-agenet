package io.github.cjlab.agent.server.api;

import io.github.cjlab.agent.core.chat.AgentService;
import io.github.cjlab.agent.core.chat.ChatRequest;
import io.github.cjlab.agent.core.chat.ChatResponse;
import io.github.cjlab.agent.core.chat.ChatUser;
import io.github.cjlab.agent.server.security.CurrentUser;
import io.github.cjlab.agent.server.security.UserConversationIds;
import io.github.cjlab.agent.server.security.CurrentUserContext;
import jakarta.annotation.PreDestroy;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final AgentService agentService;
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    public ChatController(AgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping
    public ChatResponse chat(@RequestBody ChatRequest request) {
        return chatForCurrentUser(request, CurrentUserContext.required());
    }

    @PostMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestBody ChatRequest request) {
        SseEmitter emitter = new SseEmitter(120_000L);
        CurrentUser user = CurrentUserContext.required();
        executorService.execute(() -> {
            try {
                CurrentUserContext.set(user);
                String externalConversationId = UserConversationIds.external(request == null ? null : request.conversationId());
                send(emitter, "start", new ChatStreamEvent(externalConversationId, ""));
                ChatResponse response = streamChatForCurrentUser(request, user, chunk -> sendUnchecked(
                        emitter,
                        "delta",
                        new ChatStreamEvent(externalConversationId, chunk)
                ));
                send(emitter, "done", new ChatStreamEvent(response.conversationId(), response.content()));
                emitter.complete();
            } catch (Exception exception) {
                try {
                    send(emitter, "error", new ChatStreamEvent(
                            request == null ? null : request.conversationId(),
                            exception.getMessage()
                    ));
                } catch (Exception ignored) {
                    // The client may already be disconnected.
                }
                emitter.completeWithError(exception);
            } finally {
                CurrentUserContext.clear();
            }
        });
        return emitter;
    }

    private ChatResponse chatForCurrentUser(ChatRequest request, CurrentUser user) {
        String externalConversationId = UserConversationIds.external(request == null ? null : request.conversationId());
        ChatResponse response = agentService.chat(new ChatRequest(
                UserConversationIds.internal(user.id(), externalConversationId),
                request == null ? null : request.message(),
                toChatUser(user)
        ));
        return new ChatResponse(externalConversationId, response.content());
    }

    private ChatResponse streamChatForCurrentUser(
            ChatRequest request,
            CurrentUser user,
            java.util.function.Consumer<String> chunkConsumer
    ) {
        String externalConversationId = UserConversationIds.external(request == null ? null : request.conversationId());
        ChatResponse response = agentService.streamChat(new ChatRequest(
                UserConversationIds.internal(user.id(), externalConversationId),
                request == null ? null : request.message(),
                toChatUser(user)
        ), chunkConsumer);
        return new ChatResponse(externalConversationId, response.content());
    }

    private ChatUser toChatUser(CurrentUser user) {
        return new ChatUser(
                user.id(),
                user.email(),
                user.displayName(),
                user.status() == null ? null : user.status().name()
        );
    }

    private void send(SseEmitter emitter, String eventName, ChatStreamEvent event) throws IOException {
        emitter.send(SseEmitter.event()
                .name(eventName)
                .data(event));
    }

    private void sendUnchecked(SseEmitter emitter, String eventName, ChatStreamEvent event) {
        try {
            send(emitter, eventName, event);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to send SSE event.", exception);
        }
    }

    @PreDestroy
    public void shutdownExecutor() {
        executorService.shutdown();
    }

    public record ChatStreamEvent(String conversationId, String content) {
    }
}
