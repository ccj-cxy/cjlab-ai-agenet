package io.github.cjlab.agent.server.api;

import io.github.cjlab.agent.core.chat.ChatRoleCard;
import io.github.cjlab.agent.server.security.CurrentUser;
import io.github.cjlab.agent.server.security.CurrentUserContext;
import io.github.cjlab.agent.user.UserRoleCard;
import io.github.cjlab.agent.user.UserRoleCardRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/role-cards")
public class RoleCardController {

    private final UserRoleCardRepository userRoleCardRepository;

    public RoleCardController(UserRoleCardRepository userRoleCardRepository) {
        this.userRoleCardRepository = userRoleCardRepository;
    }

    @GetMapping
    public List<RoleCardResponse> list() {
        CurrentUser user = CurrentUserContext.required();
        return userRoleCardRepository.listByUserId(user.id())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @PostMapping
    public RoleCardResponse save(@RequestBody ChatRoleCard request) {
        CurrentUser user = CurrentUserContext.required();
        ChatRoleCard roleCard = sanitize(request);
        UserRoleCard saved = userRoleCardRepository.save(new UserRoleCard(
                user.id(),
                roleCard.id(),
                roleCard.name(),
                roleCard.description(),
                roleCard.instruction(),
                Instant.now()
        ));
        return toResponse(saved);
    }

    private RoleCardResponse toResponse(UserRoleCard roleCard) {
        return new RoleCardResponse(
                roleCard.roleId(),
                roleCard.name(),
                roleCard.description(),
                roleCard.instruction(),
                roleCard.updatedAt()
        );
    }

    private ChatRoleCard sanitize(ChatRoleCard roleCard) {
        String id = normalizeId(roleCard == null ? null : roleCard.id());
        String name = trimToDefault(roleCard == null ? null : roleCard.name(), 80, "未命名角色");
        String description = trimToNull(roleCard == null ? null : roleCard.description(), 240);
        String instruction = trimToNull(roleCard == null ? null : roleCard.instruction(), 1200);
        return new ChatRoleCard(id, name, description, instruction);
    }

    private String normalizeId(String value) {
        String normalized = value == null ? "" : value.trim()
                .toLowerCase()
                .replaceAll("[^a-z0-9_-]+", "-")
                .replaceAll("^-+|-+$", "");
        if (normalized.isBlank()) {
            return "role-" + System.currentTimeMillis();
        }
        return normalized.length() <= 64 ? normalized : normalized.substring(0, 64);
    }

    private String trimToDefault(String value, int maxLength, String defaultValue) {
        String trimmed = trimToNull(value, maxLength);
        return trimmed == null ? defaultValue : trimmed;
    }

    private String trimToNull(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, maxLength);
    }

    public record RoleCardResponse(
            String id,
            String name,
            String description,
            String instruction,
            Instant updatedAt
    ) {
    }
}
