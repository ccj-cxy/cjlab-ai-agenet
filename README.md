# cjlab-ai-agent

`cjlab-ai-agent` 是一个基于 Spring Boot 和 Spring AI Alibaba 的 Java AI Agent 项目骨架。项目采用 Maven 多模块拆分，当前已经包含 Agent 核心编排、会话记忆、工具调用、RAG 知识检索、模型适配和 Web API 层。

当前默认运行模式使用本地 mock 模型，方便在没有 DashScope API Key 的情况下启动和调试；启用 `dashscope` profile 后可以切换到 Spring AI Alibaba + DashScope。

## 技术栈

- Java 21
- Spring Boot 4.1.0
- Spring AI Alibaba 1.1.2.3
- Maven 多模块
- DashScope / 通义千问，按 profile 启用
- 邮箱注册登录用户模块
- MySQL/MyBatis-Plus 持久化用户、会话记忆和知识库；Tool Registry 当前仍为内存注册表

## 完整架构说明

更详细的模块说明、类职责、调用链路和扩展路线见：

- [docs/project-architecture.md](docs/project-architecture.md)
- [docs/user-management.md](docs/user-management.md)

用户管理 MySQL 建表语句：

- [docs/sql/user-schema.sql](docs/sql/user-schema.sql)

## 模块结构

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

### `cjlab-ai-agent-common`

通用基础模块。

当前包含：

- 通用异常 `AgentException`

后续适合放：

- 统一错误码
- 通用返回结构
- 通用常量
- 基础工具类

### `cjlab-ai-agent-memory`

会话记忆模块，负责保存和读取对话历史。

当前包含：

- `ConversationMemory`：会话记忆接口
- `MyBatisPlusConversationMemory`：MySQL/MyBatis-Plus 持久化实现
- `InMemoryConversationMemory`：备用内存实现
- `ConversationMessage`：会话消息模型
- `MessageRole`：消息角色，包含 `USER`、`ASSISTANT`、`TOOL`

后续可以扩展为：

- Redis 短期记忆
- MySQL 会话持久化
- 按 token 数裁剪历史
- 会话摘要 memory

### `cjlab-ai-agent-tool`

工具调用模块，负责定义 Agent 可使用的工具。

当前包含：

- `AgentTool`：工具接口
- `ToolRegistry`：工具注册表
- `InMemoryToolRegistry`：内存版工具注册表
- `ToolRequest`：工具请求
- `ToolResult`：工具结果
- `CurrentTimeTool`：获取当前时间的示例工具

后续可以扩展为：

- Spring AI Tool Calling
- MCP Client
- HTTP API 工具
- 数据库查询工具
- 文件工具
- 浏览器工具
- 权限控制和工具调用审计

### `cjlab-ai-agent-rag`

知识库和检索模块，负责为 Agent 提供外部知识上下文。

当前包含：

- `KnowledgeRepository`：知识文档存储接口
- `MyBatisPlusKnowledgeRepository`：MySQL/MyBatis-Plus 持久化知识库
- `InMemoryKnowledgeRepository`：备用内存实现
- `KnowledgeRetriever`：检索接口
- `SimpleKeywordKnowledgeRetriever`：简单关键词检索器
- `KnowledgeDocument`：知识文档模型
- `RetrievedDocument`：检索结果模型

当前检索实现是轻量关键词匹配，适合项目骨架阶段验证链路。后续可以替换为：

- Spring AI VectorStore
- PostgreSQL + pgvector
- Milvus
- Qdrant
- Elasticsearch
- Embedding + Rerank
- 文档上传、切分、清洗、索引

### `cjlab-ai-agent-core`

Agent 核心编排模块，负责组织一次用户请求的完整执行流程。

当前包含：

- `AgentService`：Agent 服务接口
- `DefaultAgentService`：默认 Agent 编排实现
- `ChatModelGateway`：模型调用抽象
- `ChatRequest`：聊天请求
- `ChatResponse`：聊天响应

当前 `DefaultAgentService` 的执行流程：

```text
接收用户消息
-> 写入会话记忆
-> 查询最近会话历史
-> 执行 RAG 检索
-> 获取可用工具说明
-> 组装 Prompt
-> 调用 ChatModelGateway
-> 保存助手回复
-> 返回响应
```

设计原则：

- `core` 不直接依赖 Spring AI Alibaba
- `core` 不直接依赖数据库、Redis、HTTP Client
- `core` 只依赖抽象接口和领域能力模块

### `cjlab-ai-agent-infrastructure`

基础设施模块，负责接入外部框架、模型服务和持久化实现。

当前包含：

- `SpringAiChatModelGateway`：基于 Spring AI `ChatClient` 的模型适配器
- Spring AI Alibaba DashScope starter 依赖

后续适合放：

- DashScope 模型适配
- OpenAI / Azure OpenAI 适配
- Redis Memory 实现
- MySQL Repository 实现
- VectorStore 实现
- MCP Client 实现
- 外部 HTTP 工具实现

### `cjlab-ai-agent-server`

Web 启动模块，负责 Spring Boot 应用入口、Bean 装配和 REST API。

当前包含：

- `CjlabAiAgentApplication`：启动类
- `AgentConfiguration`：核心 Bean 装配
- `ChatController`：聊天接口
- `MemoryController`：会话记忆接口
- `KnowledgeController`：知识库接口
- `ToolController`：工具接口

## 依赖方向

模块依赖方向如下：

```text
server
  -> infrastructure
  -> core
  -> memory
  -> tool
  -> rag
  -> common

infrastructure
  -> core
  -> common

core
  -> memory
  -> tool
  -> rag
  -> common

memory/tool/rag
  -> common

common
  -> 无业务依赖
```

核心约束：

- `common` 不依赖任何业务模块
- `memory`、`tool`、`rag` 不依赖 `server`
- `core` 不依赖 `server` 和具体基础设施实现
- `infrastructure` 实现外部能力，不能反向依赖 `server`
- `server` 只负责 API 和装配，不承载复杂业务逻辑

## 包名规范

项目源码主包名：

```text
io.github.cjlab.agent
```

Maven 主工程坐标：

```xml
<groupId>io.github.cjlab</groupId>
<artifactId>cjlab-ai-agent</artifactId>
```

## 环境要求

- JDK 21+
- Maven 3.9+
- 可选：DashScope API Key

检查环境：

```bash
java -version
mvn -version
```

## 编译项目

```bash
mvn clean compile
```

打包并安装到本地 Maven 仓库：

```bash
mvn install -DskipTests
```

## 启动项目

### 本地 mock 模式

默认模式不需要 DashScope API Key，适合本地开发和接口调试。

```bash
mvn install -DskipTests
mvn -pl cjlab-ai-agent-server spring-boot:run
```

启动后访问：

```text
http://localhost:8080
```

### DashScope 模式

Windows PowerShell：

```powershell
$env:DASHSCOPE_API_KEY="your-api-key"
mvn -pl cjlab-ai-agent-server spring-boot:run -Dspring-boot.run.profiles=dashscope
```

Windows CMD：

```cmd
set DASHSCOPE_API_KEY=your-api-key
mvn -pl cjlab-ai-agent-server spring-boot:run -Dspring-boot.run.profiles=dashscope
```

Linux / macOS：

```bash
export DASHSCOPE_API_KEY=your-api-key
mvn -pl cjlab-ai-agent-server spring-boot:run -Dspring-boot.run.profiles=dashscope
```

## 配置说明

默认配置文件：

```text
cjlab-ai-agent-server/src/main/resources/application.yml
```

DashScope 配置文件：

```text
cjlab-ai-agent-server/src/main/resources/application-dashscope.yml
```

默认模式会关闭 DashScope 的自动模型能力，避免没有 API Key 时启动失败：

```yaml
spring:
  ai:
    model:
      chat: none
```

启用 `dashscope` profile 后会使用：

```yaml
spring:
  ai:
    model:
      chat: dashscope
    dashscope:
      api-key: ${DASHSCOPE_API_KEY}
```

## API 示例

### 聊天接口

请求：

```bash
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d "{\"conversationId\":\"demo\",\"message\":\"hello\"}"
```

响应示例：

```json
{
  "conversationId": "demo",
  "content": "Local mock response: ..."
}
```

如果 `conversationId` 为空，系统会自动生成一个会话 ID。

### 查询会话记忆

```bash
curl "http://localhost:8080/api/memory/demo?limit=20"
```

响应示例：

```json
[
  {
    "conversationId": "demo",
    "role": "USER",
    "content": "hello",
    "createdAt": "2026-06-23T14:00:00Z"
  }
]
```

### 写入知识文档

```bash
curl -X POST http://localhost:8080/api/knowledge \
  -H "Content-Type: application/json" \
  -d "{\"title\":\"Project\",\"content\":\"CJLab AI Agent uses Spring AI Alibaba.\"}"
```

响应示例：

```json
{
  "id": "generated-id",
  "title": "Project",
  "content": "CJLab AI Agent uses Spring AI Alibaba.",
  "metadata": {}
}
```

### 查询知识文档列表

```bash
curl http://localhost:8080/api/knowledge
```

### 检索知识文档

```bash
curl "http://localhost:8080/api/knowledge/search?query=Spring%20AI&limit=5"
```

### 查询工具列表

```bash
curl http://localhost:8080/api/tools
```

响应示例：

```json
[
  {
    "name": "current_time",
    "description": "Get current server time in ISO-8601 format."
  }
]
```

### 执行工具

```bash
curl -X POST http://localhost:8080/api/tools/current_time/execute \
  -H "Content-Type: application/json" \
  -d "{\"conversationId\":\"demo\",\"input\":\"now\",\"arguments\":{}}"
```

响应示例：

```json
{
  "toolName": "current_time",
  "content": "2026-06-23T22:00:00+08:00"
}
```

## 当前能力边界

当前项目是 Agent 工程骨架，不是完整生产系统。已经打通了以下链路：

```text
HTTP API
-> AgentService
-> Memory
-> RAG Retriever
-> Tool Registry
-> ChatModelGateway
-> Response
```

当前会话记忆和知识库已经接入 MySQL/MyBatis-Plus：

- 应用重启后会话记录丢失
- 应用重启后知识文档丢失
- RAG 是关键词检索，不是向量检索
- 工具调用还没有接入模型原生 function calling
- 没有权限、审计、限流和用户隔离

## 后续扩展建议

### 第一阶段：补齐持久化

- Memory 接 Redis 或 MySQL
- KnowledgeRepository 已接 MySQL，后续可继续增加向量索引
- 增加 Flyway / Liquibase 数据库迁移
- 增加用户 ID、租户 ID、会话标题

### 第二阶段：升级 RAG

- 增加文档上传接口
- 增加文档切分 Chunker
- 接入 EmbeddingModel
- 接入 Spring AI VectorStore
- 使用 pgvector、Milvus 或 Qdrant
- 增加 Rerank

### 第三阶段：工具调用

- 将 `AgentTool` 适配为 Spring AI Tool Calling
- 增加工具权限控制
- 增加工具调用日志
- 增加 MCP Client
- 支持外部工具动态注册

### 第四阶段：Agent 编排

- 增加 Planner
- 增加多步骤任务状态
- 增加任务中断和恢复
- 增加 SSE 流式响应
- 引入 Spring AI Alibaba Graph 或 Agent Framework

### 第五阶段：生产化

- 接入 OpenTelemetry
- 增加 traceId
- 增加 eval 测试集
- 增加 prompt 版本管理
- 增加模型调用成本统计
- 增加接口鉴权和限流

## 常用命令

全量编译：

```bash
mvn clean compile
```

跳过测试安装：

```bash
mvn install -DskipTests
```

运行 server 模块：

```bash
mvn -pl cjlab-ai-agent-server spring-boot:run
```

运行 server 模块并带 profile：

```bash
mvn -pl cjlab-ai-agent-server spring-boot:run -Dspring-boot.run.profiles=dashscope
```

接 OpenAI 兼容中转站：

```powershell
$env:CJLAB_OPENAI_BASE_URL="https://your-relay.example.com"
$env:CJLAB_OPENAI_API_KEY="your-relay-api-key"
$env:CJLAB_OPENAI_MODEL="gpt-4o-mini"
mvn -pl cjlab-ai-agent-server spring-boot:run -Dspring-boot.run.profiles=openai-compatible
```

如果中转站的 Chat Completions 路径不同，额外配置：

```powershell
$env:CJLAB_OPENAI_CHAT_COMPLETIONS_PATH="/v1/chat/completions"
```

只编译 server 及其依赖模块：

```bash
mvn -pl cjlab-ai-agent-server -am compile
```

## 注意事项

- `spring-ai-alibaba-bom:1.1.2.3` 当前没有管理 `spring-ai-alibaba-starter-dashscope` 的版本，因此项目在 `cjlab-ai-agent-infrastructure` 中显式声明了该依赖版本。
- 用户模块通过 `cjlab-boot-starter-mybatis-plus` 引入 MyBatis-Plus 和 MySQL 能力；该 starter 会带入 common retry 配置，因此项目排除了 Spring AI 的 `SpringAiRetryAutoConfiguration`，避免两个 `retryTemplate` Bean 同名冲突。
- 默认 mock 模式返回的是组装后的 prompt 前缀，不代表真实模型效果。真实模型请使用 `dashscope` profile 或 `openai-compatible` profile。
