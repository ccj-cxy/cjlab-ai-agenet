package io.github.cjlab.agent.server.api;

import io.github.cjlab.agent.core.chat.ChatRoleCard;
import io.github.cjlab.agent.server.security.CurrentUser;
import io.github.cjlab.agent.server.security.CurrentUserContext;
import io.github.cjlab.agent.user.UserRoleCard;
import io.github.cjlab.agent.user.UserRoleCardRepository;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/role-cards")
public class RoleCardController {

    private static final List<ChatRoleCard> DEFAULT_ROLE_CARDS = List.of(
            new ChatRoleCard(
                    "default",
                    "默认助手",
                    "平衡、直接、可执行",
                    "用清晰、简洁、务实的方式回答。优先给结论和可执行步骤。",
                    null
            ),
            new ChatRoleCard(
                    "engineer",
                    "后端工程师",
                    "Java/Spring/MyBatis 工程实现优先",
                    "以资深 Java 后端工程师风格回答。优先指出模块边界、数据模型、接口契约、异常路径和验证方式。代码建议要贴近现有工程。",
                    null
            ),
            new ChatRoleCard(
                    "product",
                    "产品分析师",
                    "目标、用户价值、流程和优先级",
                    "以产品分析师风格回答。先澄清目标和用户场景，再按优先级拆解方案，关注体验闭环、指标和风险。",
                    null
            ),
            new ChatRoleCard(
                    "teacher",
                    "讲解老师",
                    "循序渐进、示例驱动",
                    "以耐心讲解老师风格回答。把复杂概念拆成小步骤，用示例解释关键点，并在最后给出简短总结。",
                    null
            )
    );

    private final UserRoleCardRepository userRoleCardRepository;

    public RoleCardController(UserRoleCardRepository userRoleCardRepository) {
        this.userRoleCardRepository = userRoleCardRepository;
    }

    @GetMapping
    public List<RoleCardResponse> list() {
        CurrentUser user = CurrentUserContext.required();
        List<UserRoleCard> roleCards = userRoleCardRepository.listByUserId(user.id());
        if (roleCards.isEmpty()) {
            seedDefaultRoleCards(user);
            roleCards = userRoleCardRepository.listByUserId(user.id());
        }
        return roleCards
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @PostMapping("/defaults")
    public List<RoleCardResponse> saveDefaults() {
        CurrentUser user = CurrentUserContext.required();
        seedDefaultRoleCards(user);
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
                roleCard.avatar(),
                Instant.now()
        ));
        return toResponse(saved);
    }

    @DeleteMapping("/{roleId}")
    public DeleteRoleCardResponse delete(@PathVariable String roleId) {
        CurrentUser user = CurrentUserContext.required();
        String normalizedRoleId = normalizeId(roleId);
        boolean deleted = userRoleCardRepository.deleteByUserIdAndRoleId(user.id(), normalizedRoleId);
        return new DeleteRoleCardResponse(normalizedRoleId, deleted);
    }

    private RoleCardResponse toResponse(UserRoleCard roleCard) {
        return new RoleCardResponse(
                roleCard.roleId(),
                roleCard.name(),
                roleCard.description(),
                roleCard.instruction(),
                roleCard.avatar(),
                roleCard.updatedAt()
        );
    }

    private void seedDefaultRoleCards(CurrentUser user) {
        Instant now = Instant.now();
        for (ChatRoleCard roleCard : DEFAULT_ROLE_CARDS) {
            ChatRoleCard sanitized = sanitize(roleCard);
            userRoleCardRepository.save(new UserRoleCard(
                    user.id(),
                    sanitized.id(),
                    sanitized.name(),
                    sanitized.description(),
                    sanitized.instruction(),
                    sanitized.avatar(),
                    now
            ));
        }
    }

    private ChatRoleCard sanitize(ChatRoleCard roleCard) {
        String id = normalizeId(roleCard == null ? null : roleCard.id());
        String name = trimToDefault(roleCard == null ? null : roleCard.name(), 80, "未命名角色");
        String description = trimToNull(roleCard == null ? null : roleCard.description(), 240);
        String instruction = trimToNull(roleCard == null ? null : roleCard.instruction(), 1200);
        String avatar = trimAvatar(roleCard == null ? null : roleCard.avatar());
        return new ChatRoleCard(id, name, description, instruction, avatar);
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

    private String trimAvatar(String value) {
        String trimmed = trimToNull(value, 200_000);
        if (trimmed == null) {
            return null;
        }
        String lower = trimmed.toLowerCase();
        if (lower.startsWith("https://")
                || lower.startsWith("http://")
                || lower.startsWith("data:image/")) {
            return trimmed;
        }
        return null;
    }

    public record RoleCardResponse(
            String id,
            String name,
            String description,
            String instruction,
            String avatar,
            Instant updatedAt
    ) {
    }

    public record DeleteRoleCardResponse(
            String id,
            boolean deleted
    ) {
    }
}
