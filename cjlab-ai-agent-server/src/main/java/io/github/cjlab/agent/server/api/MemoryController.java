package io.github.cjlab.agent.server.api;

import io.github.cjlab.agent.memory.ConversationMemory;
import io.github.cjlab.agent.memory.ConversationMessage;
import io.github.cjlab.agent.server.security.AuthInterceptor;
import io.github.cjlab.agent.server.security.CurrentUser;
import io.github.cjlab.agent.server.security.UserConversationIds;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/memory")
public class MemoryController {

    private final ConversationMemory conversationMemory;

    public MemoryController(ConversationMemory conversationMemory) {
        this.conversationMemory = conversationMemory;
    }

    @GetMapping("/{conversationId}")
    public List<ConversationMessage> recent(
            @PathVariable String conversationId,
            @RequestParam(defaultValue = "20") int limit,
            HttpServletRequest request
    ) {
        CurrentUser user = (CurrentUser) request.getAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE);
        String externalConversationId = UserConversationIds.external(conversationId);
        return conversationMemory.recent(UserConversationIds.internal(user.id(), externalConversationId), limit)
                .stream()
                .map(message -> new ConversationMessage(
                        externalConversationId,
                        message.role(),
                        message.content(),
                        message.createdAt()
                ))
                .toList();
    }
}
