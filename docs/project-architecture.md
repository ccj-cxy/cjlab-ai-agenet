# CJLab AI Agent 项目说明文档

本文档说明 `cjlab-ai-agent` 的项目目标、模块边界、核心调用链、每个类的职责，以及当前实现的扩展方向。

## 1. 项目定位

`cjlab-ai-agent` 是一个 Java AI Agent 工程骨架，目标是把 Agent 常见能力拆成清晰的子工程：

- Chat API 和前端验证台
- Agent Runtime 抽象
- 本地 DAG 编排 Runtime
- AgentScope Runtime
- 会话记忆 Memory
- 工具注册和工具调用 Tool
- RAG 知识库检索
- 模型网关 ChatModelGateway
- DashScope 和 OpenAI-compatible 中转站适配

当前项目已经可以跑通：

```text
Browser / HTTP Client
-> ChatController
-> AgentService
-> AgentRuntime
-> Memory / RAG / Tool / LLM
-> AgentResponse
-> Browser
```

## 2. 模块结构

```text
cjlab-ai-agent
  cjlab-ai-agent-common
  cjlab-ai-agent-memory
  cjlab-ai-agent-tool
  cjlab-ai-agent-rag
  cjlab-ai-agent-user
  cjlab-ai-agent-core
  cjlab-ai-agent-infrastructure
  cjlab-ai-agent-server
```

### 2.1 `cjlab-ai-agent-common`

通用基础模块。当前只放通用异常，后续可以放错误码、统一响应、公共常量。

### 2.2 `cjlab-ai-agent-memory`

会话记忆模块。只负责保存和读取对话消息，不关心模型、工具、RAG。

### 2.3 `cjlab-ai-agent-tool`

工具模块。定义工具接口、工具注册表和工具编排器。

### 2.4 `cjlab-ai-agent-rag`

知识库检索模块。定义知识文档、知识仓库和检索器。

### 2.5 `cjlab-ai-agent-user`

用户管理模块。负责邮箱注册、邮箱密码登录、密码哈希、登录态 token 和用户资料返回。用户数据固定使用 MySQL/MyBatis-Plus 持久化。

### 2.6 `cjlab-ai-agent-core`

Agent 核心模块。定义 Chat 应用服务、Planner、DAG、Runtime 和模型调用抽象。

### 2.7 `cjlab-ai-agent-infrastructure`

基础设施适配模块。负责把核心抽象接到具体外部能力，例如 Spring AI、OpenAI-compatible HTTP API、AgentScope。

### 2.8 `cjlab-ai-agent-server`

启动和 Web 层模块。负责 Spring Boot 启动、Bean 装配、REST API、SSE API 和静态前端页面。

## 3. 核心调用链

### 3.1 非流式 Chat 调用链

接口：

```text
POST /api/chat
```

调用链：

```text
ChatController.chat
-> AgentService.chat
-> DefaultAgentService.chat
-> ConversationMemory.append(USER)
-> AgentRuntime.run
-> ConversationMemory.append(ASSISTANT)
-> ChatResponse
```

如果当前 runtime 是 `local-dag`：

```text
LocalDagAgentRuntime.run
-> Planner.plan
-> TopologicalDagExecutor.execute
-> memory node
-> retrieval node
-> tool node
-> generation node
-> ChatModelGateway.chat
-> AgentRunResult
```

如果当前 runtime 是 `agentscope`：

```text
AgentScopeAgentRuntime.run
-> 构造 ReActAgent
-> 注入 AgentScopeChatModelAdapter
-> 注入 AgentScopeRagTool
-> 加载 ConversationMemory 历史
-> agent.call
-> AgentRunResult
```

### 3.2 SSE 流式 Chat 调用链

接口：

```text
POST /api/chat/stream
Accept: text/event-stream
```

调用链：

```text
ChatController.stream
-> 后台线程调用 AgentService.chat
-> 拿到完整 ChatResponse
-> SSE event: start
-> SSE event: delta
-> SSE event: done
```

当前 SSE 是交互层流式：后端先得到完整回答，再按 12 字符分片推给浏览器。也就是说，前端体验是流式，但模型网关还不是 token 级原生流式。

后续如果要改成真正 token 流，需要把：

```text
ChatModelGateway.chat(String)
```

扩展为类似：

```text
ChatModelGateway.streamChat(String, Consumer<String>)
```

然后让 DashScope 或 OpenAI-compatible 网关直接读取模型流并转发 SSE。

## 4. 类职责说明

## 4.1 Common 模块

### `AgentException`

位置：

```text
cjlab-ai-agent-common/src/main/java/io/github/cjlab/agent/common/AgentException.java
```

职责：

- 项目内部通用运行时异常。
- 用于抛出业务错误，例如消息为空、DAG 依赖错误、工具不存在、模型调用失败。

被使用位置：

- `DefaultAgentService`：校验消息为空。
- `TopologicalDagExecutor`：DAG 节点重复、循环依赖。
- `LocalDagAgentRuntime`：generation 节点没有结果。
- `ToolController`：工具不存在。
- `OpenAiCompatibleChatModelGateway`：中转站请求失败。
- `AgentScopeAgentRuntime`：AgentScope 执行失败。

## 4.2 Memory 模块

### `ConversationMemory`

职责：

- 会话记忆接口。
- 定义两件事：
  - `append`：追加一条消息。
  - `recent`：按会话 ID 读取最近 N 条消息。

衔接方式：

- `DefaultAgentService` 写入用户消息和助手消息。
- `LocalDagAgentRuntime` 的 memory 节点读取历史。
- `AgentScopeAgentRuntime` 读取历史并转换成 AgentScope 消息。
- `MemoryController` 暴露读取接口给前端。

### `ConversationMessage`

职责：

- 一条会话消息的数据结构。
- 字段：
  - `conversationId`：会话 ID。
  - `role`：消息角色。
  - `content`：消息内容。
  - `createdAt`：创建时间。

衔接方式：

- `DefaultAgentService` 创建 `USER` 和 `ASSISTANT` 消息。
- `InMemoryConversationMemory` 保存该对象。
- `MemoryController` 直接返回该对象列表。

### `MessageRole`

职责：

- 定义消息角色：
  - `USER`
  - `ASSISTANT`
  - `TOOL`

衔接方式：

- `DefaultAgentService` 使用 `USER` 和 `ASSISTANT`。
- `AgentScopeAgentRuntime` 把它转换成 AgentScope 的 `MsgRole`。

### `InMemoryConversationMemory`

职责：

- `ConversationMemory` 的内存实现。
- 内部用 `ConcurrentHashMap<String, List<ConversationMessage>>` 按 `conversationId` 分组存储消息。
- `recent` 会返回最近 N 条消息。

当前限制：

- 应用重启后消息丢失。
- 每个会话的 `ArrayList` 不是严格并发安全的。
- 没有 token 裁剪、摘要、分页。

后续替换方向：

- Redis 短期记忆。
- MySQL/PostgreSQL 持久化记忆。
- 向量记忆。
- 会话摘要 Memory。

## 4.3 Tool 模块

### `AgentTool`

职责：

- Agent 可调用工具的统一接口。
- 每个工具必须提供：
  - `name`
  - `description`
  - `execute`

衔接方式：

- `CurrentTimeTool` 实现它。
- `ToolRegistry` 保存它。
- `ToolController` 手动执行它。
- `RuleBasedToolOrchestrator` 根据用户消息自动调用它。
- `AgentScopeRagTool` 同时实现 `AgentTool`，并暴露给 AgentScope。

### `ToolRequest`

职责：

- 工具执行入参。
- 字段：
  - `conversationId`
  - `input`
  - `arguments`

衔接方式：

- `ToolController.execute` 从 HTTP body 接收。
- `RuleBasedToolOrchestrator` 自动构造。
- `AgentTool.execute` 消费。

### `ToolResult`

职责：

- 工具执行结果。
- 字段：
  - `toolName`
  - `content`

衔接方式：

- `CurrentTimeTool` 返回它。
- `LocalDagAgentRuntime` 的 tool 节点收集它。
- `LocalDagAgentRuntime.buildPrompt` 把工具结果拼进 prompt。

### `ToolInvocation`

职责：

- 表示一次工具调用意图。
- 当前项目暂未在主链路中使用。

后续用途：

- Planner 输出显式工具调用计划。
- Tool calling 审计日志。
- 多工具编排 DAG 节点入参。

### `ToolRegistry`

职责：

- 工具注册表接口。
- 提供：
  - `list`
  - `findByName`

衔接方式：

- `AgentConfiguration` 创建 `InMemoryToolRegistry`。
- `ToolController` 查询工具列表和执行工具。
- `RuleBasedToolOrchestrator` 根据名称查找工具。
- `AgentScopeAgentRuntime` 读取工具名称写入 metadata。

### `InMemoryToolRegistry`

职责：

- 内存版工具注册表。
- 内部使用 `CopyOnWriteArrayList` 保存工具。

当前默认工具：

- `CurrentTimeTool`

### `CurrentTimeTool`

职责：

- 示例工具。
- 名称：`current_time`
- 返回服务器当前时间，格式是 `OffsetDateTime.now().toString()`。

衔接方式：

- 由 `AgentConfiguration.toolRegistry` 注册。
- 用户消息包含 `time`、`时间`、`now` 时会被 `RuleBasedToolOrchestrator` 调用。

### `ToolOrchestrator`

职责：

- 工具编排接口。
- 输入会话 ID 和用户消息，输出工具结果列表。

衔接方式：

- `LocalDagAgentRuntime` 的 tool 节点调用它。

### `RuleBasedToolOrchestrator`

职责：

- 当前工具编排实现。
- 基于规则判断是否调用工具：
  - 包含 `time`
  - 包含 `时间`
  - 包含 `now`
- 命中后调用 `current_time` 工具。

当前限制：

- 不是 LLM function calling。
- 不能自动选择多个复杂工具。
- 没有参数抽取能力。

## 4.4 RAG 模块

### `KnowledgeDocument`

职责：

- 知识库文档模型。
- 字段：
  - `id`
  - `title`
  - `content`
  - `metadata`

衔接方式：

- `KnowledgeController.save` 接收并保存。
- `KnowledgeRepository` 存储。
- `KnowledgeRetriever` 检索。
- `LocalDagAgentRuntime` 把检索结果拼进 prompt。

### `KnowledgeRepository`

职责：

- 知识文档仓库接口。
- 提供：
  - `save`
  - `list`
  - `findById`

衔接方式：

- `KnowledgeController` 用它保存和列出知识。
- `KnowledgeRetriever` 用它读取全部文档做检索。

### `InMemoryKnowledgeRepository`

职责：

- 内存版知识仓库。
- 内部用 `ConcurrentHashMap<String, KnowledgeDocument>` 保存文档。

当前限制：

- 应用重启后知识丢失。
- 没有文档分片。
- 没有向量索引。

### `KnowledgeRetriever`

职责：

- RAG 检索接口。
- 输入 query 和 limit，输出相关文档列表。

衔接方式：

- `KnowledgeController.search` 暴露 HTTP 检索。
- `LocalDagAgentRuntime` 的 retrieval 节点调用。
- `AgentScopeRagTool` 调用。

### `RetrievedDocument`

职责：

- 检索结果模型。
- 包含：
  - `KnowledgeDocument`
  - `score`

### `SimpleKeywordKnowledgeRetriever`

职责：

- 简单关键词检索器。
- 按空格拆 query，用标题和内容是否包含关键词来打分。

当前状态：

- 项目中保留作为简单实现参考。
- 当前 Spring 装配默认使用 `Bm25KnowledgeRetriever`。

### `Bm25KnowledgeRetriever`

职责：

- 当前默认 RAG 检索实现。
- 使用 BM25 思路计算 query 与文档标题/内容的相关性。

核心逻辑：

- `tokenize`：把文本拆成英文、数字、中文字符 token。
- `documentFrequency`：计算每个词出现在多少文档中。
- `score`：按 BM25 公式计算相关分。
- `retrieve`：排序后返回前 N 个非零分文档。

当前限制：

- 仍然是关键词级检索。
- 没有 embedding。
- 没有 chunk。
- 没有 rerank。

## 4.5 Core Chat 模块

### `AgentService`

职责：

- Chat 应用服务接口。
- 当前只有一个方法：`chat(ChatRequest)`。

衔接方式：

- `ChatController` 调用。
- `DefaultAgentService` 实现。

### `ChatRequest`

职责：

- Chat 请求模型。
- 字段：
  - `conversationId`
  - `message`

### `ChatResponse`

职责：

- Chat 响应模型。
- 字段：
  - `conversationId`
  - `content`

### `DefaultAgentService`

职责：

- Chat 主应用服务实现。
- 做三件事：
  - 校验消息不能为空。
  - 写入用户消息到 `ConversationMemory`。
  - 调用 `AgentRuntime.run`。
  - 写入助手回复到 `ConversationMemory`。

为什么它不直接调用 LLM：

- 这是为了把“会话生命周期”和“Agent 执行策略”分开。
- `DefaultAgentService` 只负责应用流程。
- 真正如何规划、检索、调用工具、生成回复，由 `AgentRuntime` 决定。

## 4.6 Core LLM 模块

### `ChatModelGateway`

职责：

- 模型调用抽象。
- 当前只有：

```java
String chat(String message);
```

衔接方式：

- `LocalDagAgentRuntime` 的 generation 节点调用。
- `AgentScopeChatModelAdapter` 调用。
- 具体实现由 profile 决定：
  - 默认 mock lambda
  - `SpringAiChatModelGateway`
  - `OpenAiCompatibleChatModelGateway`

当前限制：

- 只有非流式接口。
- SSE 目前由 Controller 分片模拟。
- 后续应扩展原生 stream。

## 4.7 Core Planner 模块

### `PlanningContext`

职责：

- Planner 的输入上下文。
- 包含：
  - `conversationId`
  - `userMessage`

### `PlanStepType`

职责：

- 计划步骤类型枚举。
- 当前包括：
  - `MEMORY`
  - `RETRIEVAL`
  - `TOOL`
  - `GENERATION`

### `PlanStep`

职责：

- 单个计划步骤。
- 字段：
  - `id`：节点 ID。
  - `type`：节点类型。
  - `dependsOn`：依赖哪些步骤。
  - `parameters`：节点参数。

### `AgentPlan`

职责：

- Planner 输出的完整计划。
- 字段：
  - `goal`
  - `steps`

### `Planner`

职责：

- Planner 接口。
- 输入 `PlanningContext`，输出 `AgentPlan`。

### `DefaultPlanner`

职责：

- 当前默认 Planner。
- 固定输出 4 个步骤：

```text
memory
retrieval
tool
generation
```

依赖关系：

```text
memory      no dependency
retrieval   no dependency
tool        no dependency
generation  depends on memory, retrieval, tool
```

当前限制：

- 不是 LLM 动态规划。
- 不会根据任务复杂度改变 DAG。
- 不会做多轮工具调用。

## 4.8 Core DAG 模块

### `DagExecutionContext`

职责：

- DAG 执行上下文。
- 保存：
  - `conversationId`
  - `userMessage`
  - 每个节点的执行结果

衔接方式：

- `TopologicalDagExecutor` 执行节点时写入结果。
- `LocalDagAgentRuntime.buildPrompt` 从中读取 memory、retrieval、tool 结果。

### `DagNode`

职责：

- DAG 节点模型。
- 字段：
  - `id`
  - `dependsOn`
  - `handler`
  - `parameters`

### `DagNodeHandler`

职责：

- 节点处理函数接口。
- 当前是：

```java
Object handle(DagExecutionContext context);
```

### `DagExecutor`

职责：

- DAG 执行器接口。

### `TopologicalDagExecutor`

职责：

- 基于拓扑排序执行 DAG。
- 执行流程：
  - 收集所有节点。
  - 找出依赖已完成的 ready 节点。
  - 执行 ready 节点。
  - 写入结果。
  - 直到所有节点完成。

错误处理：

- 节点 ID 重复会抛 `AgentException`。
- 没有 ready 节点说明存在循环依赖或缺失依赖，也会抛 `AgentException`。

当前限制：

- ready 节点是顺序执行，不是并行执行。
- 没有节点超时。
- 没有节点重试。
- 没有节点状态持久化。

## 4.9 Core Runtime 模块

### `AgentRunRequest`

职责：

- Runtime 输入。
- 包含：
  - `conversationId`
  - `message`

### `AgentRunResult`

职责：

- Runtime 输出。
- 包含：
  - `conversationId`
  - `content`
  - `metadata`

metadata 用途：

- 记录 runtime 名称。
- 记录 plan。
- 记录 dagResults。
- 记录工具名称等调试信息。

### `AgentRuntime`

职责：

- Agent 执行引擎抽象。
- 当前有两个实现：
  - `LocalDagAgentRuntime`
  - `AgentScopeAgentRuntime`

### `LocalDagAgentRuntime`

职责：

- 本地 DAG Agent Runtime。
- 是当前默认 runtime。

完整执行过程：

```text
1. planner.plan(...)
2. 把 AgentPlan 转成 DagNode 集合
3. dagExecutor.execute(...)
4. memory 节点读取最近会话
5. retrieval 节点检索知识库
6. tool 节点执行工具编排
7. generation 节点构造 prompt 并调用 ChatModelGateway
8. 从 generation 节点结果拿到最终回答
9. 返回 AgentRunResult
```

`buildPrompt` 拼接内容：

- 系统说明
- Conversation history
- Retrieved knowledge
- Tool results
- User message

它衔接了核心能力：

```text
ConversationMemory
KnowledgeRetriever
ToolOrchestrator
ChatModelGateway
Planner
DagExecutor
```

## 4.10 Infrastructure LLM 模块

### `SpringAiChatModelGateway`

职责：

- 把 `ChatModelGateway` 适配到 Spring AI `ChatClient`。

启用条件：

- `dashscope` profile。

调用方式：

```text
chatClient.prompt()
  .user(message)
  .call()
  .content()
```

衔接方式：

- Spring AI Alibaba DashScope starter 创建 `ChatClient.Builder`。
- `AgentConfiguration` 在 `dashscope` profile 下创建该 Gateway。
- `LocalDagAgentRuntime` 或 `AgentScopeChatModelAdapter` 调用它。

### `OpenAiCompatibleChatModelGateway`

职责：

- 项目内置 OpenAI-compatible HTTP 网关。
- 用于接中转站，例如兼容 `/v1/chat/completions` 的服务。

配置前缀：

```yaml
cjlab.openai-compatible
```

请求格式：

```json
{
  "model": "...",
  "temperature": 0.7,
  "messages": [
    {
      "role": "user",
      "content": "..."
    }
  ]
}
```

响应解析：

- 优先读取 `choices[0].message.content`。
- 兼容读取 `choices[0].text`。

为什么不用 Spring AI OpenAI starter：

- 当前项目是 Spring Boot 4.1 / Spring Framework 7。
- Spring AI OpenAI 1.1.2 与当前 Spring Web 方法签名存在兼容问题。
- 因此这里用 Java 21 `HttpClient` 直接接中转站。

## 4.11 Infrastructure AgentScope 模块

### `AgentScopeAgentRuntime`

职责：

- `AgentRuntime` 的 AgentScope 实现。
- 内部构造 AgentScope `ReActAgent`。

执行流程：

```text
1. 创建 Toolkit
2. 注册 AgentScopeRagTool
3. 创建 ReActAgent
4. 注入 AgentScopeChatModelAdapter
5. 注入 InMemoryMemory
6. 把 ConversationMemory 转为 AgentScope Msg
7. agent.call(...)
8. 返回 AgentRunResult
```

启用方式：

```text
cjlab.agent.runtime=agentscope
```

### `AgentScopeChatModelAdapter`

职责：

- 把项目的 `ChatModelGateway` 适配成 AgentScope 的 `Model`。
- AgentScope 调模型时，最终仍然回到项目统一的模型网关。

好处：

- AgentScope runtime 可以复用 DashScope、中转站或 mock 模型。
- 模型切换不影响 AgentScope runtime 代码。

### `AgentScopeRagTool`

职责：

- 把项目 `KnowledgeRetriever` 包装成 AgentScope 可用工具。
- 工具名：`rag_search`。

它同时支持两种调用方式：

- 作为项目 `AgentTool` 被调用。
- 通过 `@Tool` 注解被 AgentScope Toolkit 识别。

### `AgentScopeMemoryAdapter`

职责：

- 简单包装 `ConversationMemory`。
- 当前不是主链路必需类，主要作为 AgentScope memory 适配预留。

### `AgentScopeToolAdapter`

职责：

- 简单包装 `ToolRegistry`。
- 当前用于预留把项目工具集适配到 AgentScope 的扩展点。

## 4.12 Server 模块

### `CjlabAiAgentApplication`

职责：

- Spring Boot 启动类。
- 使用 Spring Boot 默认组件扫描，只扫描 `io.github.cjlab.agent.server` 及其子包。
- 其他子工程的 Bean 不靠全局扫描注入，而是由 `AgentConfiguration` 显式装配。
- MyBatis Mapper 由 `UserMybatisConfiguration` 单独扫描 `io.github.cjlab.agent.user.persistence.mapper`。

### `AgentConfiguration`

职责：

- 项目的核心 Bean 装配类。

它决定：

- 当前使用哪个 `ChatModelGateway`。
- 当前使用哪个 `ConversationMemory`。
- 当前使用哪个 `KnowledgeRepository`。
- 当前使用哪个 `KnowledgeRetriever`。
- 当前注册哪些工具。
- 当前使用哪个 `AgentRuntime`。

模型网关装配规则：

```text
dashscope profile
-> SpringAiChatModelGateway

openai-compatible profile
-> OpenAiCompatibleChatModelGateway

其他 profile
-> local mock ChatModelGateway
```

Runtime 装配规则：

```text
cjlab.agent.runtime=local-dag
-> LocalDagAgentRuntime

cjlab.agent.runtime=agentscope
-> AgentScopeAgentRuntime
```

默认：

```text
cjlab.agent.runtime=local-dag
```

### `ChatController`

职责：

- Chat HTTP API。

接口：

```text
POST /api/chat
POST /api/chat/stream
```

`/api/chat`：

- 普通 JSON 请求响应。
- 返回 `ChatResponse`。

`/api/chat/stream`：

- 返回 SSE。
- 使用 `SseEmitter`。
- 后台线程调用 `AgentService.chat`。
- 分片发送：
  - `start`
  - `delta`
  - `done`
  - `error`

当前限制：

- 不是模型原生 token 流。
- 是完整回答分片后的 SSE。

### `MemoryController`

职责：

- 暴露会话记忆查询接口。

接口：

```text
GET /api/memory/{conversationId}?limit=20
```

用途：

- 前端 Memory Tab。
- 调试会话是否写入成功。

### `KnowledgeController`

职责：

- 暴露知识库管理和检索接口。

接口：

```text
POST /api/knowledge
GET /api/knowledge
GET /api/knowledge/search?query=...&limit=5
```

衔接方式：

- `POST` 保存文档到 `KnowledgeRepository`。
- `GET` 列出所有文档。
- `/search` 调用 `KnowledgeRetriever`。

### `ToolController`

职责：

- 暴露工具列表和工具手动执行接口。

接口：

```text
GET /api/tools
POST /api/tools/{name}/execute
```

用途：

- 前端 Tools Tab。
- 验证工具是否注册成功。
- 手动执行工具，绕过 Agent 自动编排。

### `RuntimeController`

职责：

- 暴露当前运行时诊断信息。

接口：

```text
GET /api/runtime
```

返回：

- `runtime`
- `activeProfiles`
- `chatModel`

用途：

- 前端左侧 Runtime 状态显示。
- 判断当前跑的是 `local-dag` 还是 `agentscope`。
- 判断当前模型 profile 是否启用。

## 5. 前端验证台

位置：

```text
cjlab-ai-agent-server/src/main/resources/static
```

文件：

- `index.html`
- `styles.css`
- `app.js`

功能：

- 左侧：会话管理、Runtime 状态。
- 中间：SSE 对话流。
- 右侧：Memory、RAG、Tools、Request Log。

前端对话调用：

```text
fetch("/api/chat/stream", {
  method: "POST",
  headers: {
    Accept: "text/event-stream",
    Content-Type: "application/json"
  },
  body: JSON.stringify(...)
})
```

为什么不用浏览器 `EventSource`：

- 原生 `EventSource` 只适合 GET。
- 当前 Chat 需要 POST JSON body。
- 所以前端使用 `fetch + ReadableStream` 手动解析 SSE。

## 6. 配置说明

### 6.1 默认本地 mock

默认配置：

```yaml
spring:
  ai:
    model:
      chat: none

cjlab:
  agent:
    runtime: local-dag
```

启动：

```powershell
java -jar cjlab-ai-agent-server\target\cjlab-ai-agent-server-1.0-SNAPSHOT.jar
```

特点：

- 不需要 API Key。
- 模型返回以 `Local mock response:` 开头。
- 适合验证 Memory、RAG、Tool、DAG、SSE。

### 6.2 DashScope

配置：

```powershell
$env:DASHSCOPE_API_KEY="your-api-key"
```

启动：

```powershell
java -jar cjlab-ai-agent-server\target\cjlab-ai-agent-server-1.0-SNAPSHOT.jar --spring.profiles.active=dashscope
```

### 6.3 OpenAI-compatible 中转站

配置：

```powershell
$env:CJLAB_OPENAI_BASE_URL="https://your-relay.example.com"
$env:CJLAB_OPENAI_API_KEY="your-relay-api-key"
$env:CJLAB_OPENAI_MODEL="your-model"
$env:CJLAB_OPENAI_CHAT_COMPLETIONS_PATH="/v1/chat/completions"
```

启动：

```powershell
java -jar cjlab-ai-agent-server\target\cjlab-ai-agent-server-1.0-SNAPSHOT.jar --spring.profiles.active=openai-compatible
```

### 6.4 AgentScope Runtime

启动 local mock + AgentScope：

```powershell
java -jar cjlab-ai-agent-server\target\cjlab-ai-agent-server-1.0-SNAPSHOT.jar --cjlab.agent.runtime=agentscope
```

启动中转站 + AgentScope：

```powershell
java -jar cjlab-ai-agent-server\target\cjlab-ai-agent-server-1.0-SNAPSHOT.jar `
  --spring.profiles.active=openai-compatible `
  --cjlab.agent.runtime=agentscope
```

## 7. 当前实现边界

### 7.1 Memory

- 目前是内存存储。
- 应用重启丢失。
- 没有用户隔离。
- 没有分页和摘要。

### 7.2 RAG

- 当前默认是 BM25 关键词检索。
- 没有 embedding。
- 没有向量数据库。
- 没有文档 chunk。
- 没有 rerank。

### 7.3 Tool

- 当前是规则触发工具。
- 没有模型原生 function calling。
- 没有 MCP。
- 没有工具权限控制。

### 7.4 Planner

- 当前是固定计划。
- 没有根据用户问题动态生成 DAG。
- 没有多步任务状态。
- 没有任务恢复。

### 7.5 SSE

- 当前是 Controller 层分片推送。
- 不是模型 token 级流式。

## 8. 建议演进路线

### 第一阶段：持久化

- `ConversationMemory` 接 Redis 或 MySQL。
- `KnowledgeRepository` 接数据库。
- 增加用户 ID、租户 ID、会话标题。

### 第二阶段：向量 RAG

- 增加文档上传。
- 增加 Chunker。
- 增加 EmbeddingModel。
- 接入 Milvus、Qdrant、pgvector 或 Doris Vector。
- 增加 rerank。

### 第三阶段：模型原生流式

- 扩展 `ChatModelGateway`。
- DashScope 使用原生 stream。
- OpenAI-compatible 使用 streaming chat completions。
- `ChatController.stream` 直接转发 token。

### 第四阶段：工具调用升级

- 把 `AgentTool` 适配为 Spring AI Tool Calling。
- 增加工具参数 schema。
- 增加工具执行审计。
- 接入 MCP Client。

### 第五阶段：动态 Planner

- Planner 由 LLM 生成。
- PlanStep 支持更多类型。
- DAG 节点支持条件分支。
- DAG 执行状态持久化。

## 9. 一句话总结

当前项目的主线是：

```text
Server API 接收请求
-> AgentService 负责会话生命周期
-> AgentRuntime 负责 Agent 执行策略
-> Planner 生成步骤
-> DAG 执行 Memory / RAG / Tool / Generation
-> ChatModelGateway 屏蔽具体模型供应商
-> Memory 写回助手回复
-> 前端通过 SSE 展示对话
```

这个分层的关键价值是：后续无论替换模型、替换记忆存储、升级 RAG、增强工具调用，还是切换 AgentScope Runtime，都不需要推翻整体结构。
