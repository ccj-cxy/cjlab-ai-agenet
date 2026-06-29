package io.github.cjlab.agent.infrastructure.agentscope;

import io.github.cjlab.agent.rag.KnowledgeRetriever;
import io.github.cjlab.agent.rag.RetrievedDocument;
import io.github.cjlab.agent.tool.AgentTool;
import io.github.cjlab.agent.tool.ToolRequest;
import io.github.cjlab.agent.tool.ToolResult;
import io.agentscope.core.tool.Tool;

import java.util.stream.Collectors;

public class AgentScopeRagTool implements AgentTool {

    private final KnowledgeRetriever knowledgeRetriever;

    public AgentScopeRagTool(KnowledgeRetriever knowledgeRetriever) {
        this.knowledgeRetriever = knowledgeRetriever;
    }

    @Override
    public String name() {
        return "rag_search";
    }

    @Override
    public String description() {
        return "Search project knowledge base and return relevant documents.";
    }

    @Override
    public ToolResult execute(ToolRequest request) {
        String content = knowledgeRetriever.retrieve(request.input(), 5).stream()
                .map(this::format)
                .collect(Collectors.joining("\n"));
        return new ToolResult(name(), content.isBlank() ? "No relevant knowledge found." : content);
    }

    private String format(RetrievedDocument document) {
        return "- " + document.document().title() + ": " + document.document().content();
    }

    @Tool(name = "rag_search", description = "Search project knowledge base and return relevant documents.")
    public String search(String query) {
        String content = knowledgeRetriever.retrieve(query, 5).stream()
                .map(this::format)
                .collect(Collectors.joining("\n"));
        return content.isBlank() ? "No relevant knowledge found." : content;
    }
}
