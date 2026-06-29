package io.github.cjlab.agent.core.dag;

public interface DagNodeHandler {

    Object handle(DagExecutionContext context);
}
