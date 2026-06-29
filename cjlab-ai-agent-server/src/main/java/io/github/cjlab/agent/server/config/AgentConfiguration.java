package io.github.cjlab.agent.server.config;

import io.github.cjlab.agent.core.chat.AgentService;
import io.github.cjlab.agent.core.chat.DefaultAgentService;
import io.github.cjlab.agent.core.dag.DagExecutor;
import io.github.cjlab.agent.core.dag.TopologicalDagExecutor;
import io.github.cjlab.agent.core.llm.ChatModelGateway;
import io.github.cjlab.agent.core.plan.DefaultPlanner;
import io.github.cjlab.agent.core.plan.Planner;
import io.github.cjlab.agent.core.runtime.AgentRuntime;
import io.github.cjlab.agent.core.runtime.LocalDagAgentRuntime;
import io.github.cjlab.agent.infrastructure.agentscope.AgentScopeAgentRuntime;
import io.github.cjlab.agent.infrastructure.llm.OpenAiCompatibleChatModelGateway;
import io.github.cjlab.agent.infrastructure.llm.SpringAiChatModelGateway;
import io.github.cjlab.agent.memory.ConversationMemory;
import io.github.cjlab.agent.memory.persistence.mapper.ConversationMessageMapper;
import io.github.cjlab.agent.memory.repository.mybatis.MyBatisPlusConversationMemory;
import io.github.cjlab.agent.rag.Bm25KnowledgeRetriever;
import io.github.cjlab.agent.rag.KnowledgeRepository;
import io.github.cjlab.agent.rag.KnowledgeRetriever;
import io.github.cjlab.agent.rag.persistence.mapper.KnowledgeDocumentMapper;
import io.github.cjlab.agent.rag.repository.mybatis.MyBatisPlusKnowledgeRepository;
import io.github.cjlab.agent.server.security.UserScopedKnowledgeRetriever;
import io.github.cjlab.agent.tool.CurrentTimeTool;
import io.github.cjlab.agent.tool.InMemoryToolRegistry;
import io.github.cjlab.agent.tool.RuleBasedToolOrchestrator;
import io.github.cjlab.agent.tool.ToolOrchestrator;
import io.github.cjlab.agent.tool.ToolRegistry;
import io.github.cjlab.agent.user.PasswordHasher;
import io.github.cjlab.agent.user.Pbkdf2PasswordHasher;
import io.github.cjlab.agent.user.UserRepository;
import io.github.cjlab.agent.user.UserService;
import io.github.cjlab.agent.user.UserSessionService;
import io.github.cjlab.agent.user.persistence.mapper.UserAccountMapper;
import io.github.cjlab.agent.user.persistence.mapper.UserSessionMapper;
import io.github.cjlab.agent.user.repository.mybatis.MyBatisPlusUserRepository;
import io.github.cjlab.agent.user.repository.mybatis.MyBatisPlusUserSessionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.Duration;
import java.util.List;

@Configuration
@EnableConfigurationProperties(OpenAiCompatibleChatModelGateway.Properties.class)
public class AgentConfiguration {

    @Bean
    @Profile("dashscope")
    public ChatModelGateway chatModelGateway(ChatClient.Builder chatClientBuilder) {
        return new SpringAiChatModelGateway(chatClientBuilder);
    }

    @Bean
    @Profile("openai-compatible")
    public ChatModelGateway openAiCompatibleChatModelGateway(OpenAiCompatibleChatModelGateway.Properties properties) {
        return new OpenAiCompatibleChatModelGateway(properties);
    }

    @Bean
    @Profile("!dashscope & !openai-compatible")
    public ChatModelGateway localChatModelGateway() {
        return message -> "Local mock response: " + message;
    }

    @Bean
    public ConversationMemory conversationMemory(ConversationMessageMapper conversationMessageMapper) {
        return new MyBatisPlusConversationMemory(conversationMessageMapper);
    }

    @Bean
    public KnowledgeRepository knowledgeRepository(KnowledgeDocumentMapper knowledgeDocumentMapper) {
        return new MyBatisPlusKnowledgeRepository(knowledgeDocumentMapper);
    }

    @Bean
    public KnowledgeRetriever knowledgeRetriever(KnowledgeRepository knowledgeRepository) {
        return new UserScopedKnowledgeRetriever(new Bm25KnowledgeRetriever(knowledgeRepository));
    }

    @Bean
    public ToolRegistry toolRegistry() {
        return new InMemoryToolRegistry(List.of(new CurrentTimeTool()));
    }

    @Bean
    public ToolOrchestrator toolOrchestrator(ToolRegistry toolRegistry) {
        return new RuleBasedToolOrchestrator(toolRegistry);
    }

    @Bean
    public UserRepository userRepository(UserAccountMapper userAccountMapper) {
        return new MyBatisPlusUserRepository(userAccountMapper);
    }

    @Bean
    public PasswordHasher passwordHasher() {
        return new Pbkdf2PasswordHasher();
    }

    @Bean
    public UserSessionService userSessionService(
            UserSessionMapper userSessionMapper,
            @Value("${cjlab.user.session-ttl:24h}") Duration sessionTtl
    ) {
        return new MyBatisPlusUserSessionService(userSessionMapper, sessionTtl);
    }

    @Bean
    public UserService userService(
            UserRepository userRepository,
            PasswordHasher passwordHasher,
            UserSessionService userSessionService
    ) {
        return new UserService(userRepository, passwordHasher, userSessionService);
    }

    @Bean
    public Planner planner() {
        return new DefaultPlanner();
    }

    @Bean
    public DagExecutor dagExecutor() {
        return new TopologicalDagExecutor();
    }

    @Bean
    @ConditionalOnProperty(name = "cjlab.agent.runtime", havingValue = "local-dag", matchIfMissing = true)
    public AgentRuntime localDagAgentRuntime(
            ChatModelGateway chatModelGateway,
            ConversationMemory conversationMemory,
            KnowledgeRetriever knowledgeRetriever,
            ToolOrchestrator toolOrchestrator,
            Planner planner,
            DagExecutor dagExecutor
    ) {
        return new LocalDagAgentRuntime(
                chatModelGateway,
                conversationMemory,
                knowledgeRetriever,
                toolOrchestrator,
                planner,
                dagExecutor
        );
    }

    @Bean
    @ConditionalOnProperty(name = "cjlab.agent.runtime", havingValue = "agentscope")
    public AgentRuntime agentScopeAgentRuntime(
            ChatModelGateway chatModelGateway,
            ConversationMemory conversationMemory,
            KnowledgeRetriever knowledgeRetriever,
            ToolRegistry toolRegistry
    ) {
        return new AgentScopeAgentRuntime(chatModelGateway, conversationMemory, knowledgeRetriever, toolRegistry);
    }

    @Bean
    public AgentService agentService(ConversationMemory conversationMemory, AgentRuntime agentRuntime) {
        return new DefaultAgentService(conversationMemory, agentRuntime);
    }
}
