# 配置使用手册

本文档说明 `cjlab-ai-agent` 当前项目的配置项、运行模式、环境变量、Maven profile 和常见启动方式。

## 配置文件位置

当前主要配置文件位于 `cjlab-ai-agent-server` 模块：

```text
cjlab-ai-agent-server/src/main/resources/application.yml
cjlab-ai-agent-server/src/main/resources/application-dashscope.yml
```

`application.yml` 是默认配置。

`application-dashscope.yml` 只有在启用 Spring profile `dashscope` 时生效。

## 默认配置

当前默认配置如下：

```yaml
spring:
  application:
    name: cjlab-ai-agent
  ai:
    model:
      chat: none
      embedding:
        text: none
        multimodal: none
      image: none
      video: none
      audio:
        speech: none
        transcription: none
      rerank: none
    dashscope:
      api-key: ${DASHSCOPE_API_KEY:}
      agent:
        enabled: false
    alibaba:
      tool:
        async:
          enabled: false

server:
  port: 8080

cjlab:
  agent:
    runtime: local-dag
```

默认配置的目标是：**不依赖外部模型服务，也能启动项目和调试 API**。

因此默认关闭了 Spring AI 的大部分模型自动配置：

```yaml
spring.ai.model.chat: none
spring.ai.model.embedding.text: none
spring.ai.model.embedding.multimodal: none
spring.ai.model.image: none
spring.ai.model.video: none
spring.ai.model.audio.speech: none
spring.ai.model.audio.transcription: none
spring.ai.model.rerank: none
```

默认运行时：

```yaml
cjlab.agent.runtime: local-dag
```

默认端口：

```yaml
server.port: 8080
```

## 运行模式

项目当前支持两层运行模式：

- Spring profile：控制模型服务环境，例如 `dashscope`
- Agent runtime：控制 Agent 执行引擎，例如 `local-dag`、`agentscope`

### 默认模式

默认模式不启用任何 Spring profile：

```bash
mvn -pl cjlab-ai-agent-server spring-boot:run
```

默认效果：

- 使用 `local-dag` runtime
- 使用本地 mock chat model
- 不需要 `DASHSCOPE_API_KEY`
- 适合本地开发、接口调试、DAG/Planner/RAG/Tool 链路验证

### DashScope 模式

DashScope 模式启用 `dashscope` profile：

```bash
mvn -pl cjlab-ai-agent-server spring-boot:run -Dspring-boot.run.profiles=dashscope
```

对应配置文件：

```text
application-dashscope.yml
```

内容：

```yaml
spring:
  ai:
    model:
      chat: dashscope
    dashscope:
      api-key: ${DASHSCOPE_API_KEY}
```

启用后：

- `spring.ai.model.chat` 切换为 `dashscope`
- `ChatModelGateway` 使用 Spring AI `ChatClient`
- 需要配置环境变量 `DASHSCOPE_API_KEY`

## 环境变量

### `DASHSCOPE_API_KEY`

DashScope API Key。

默认模式可以为空：

```yaml
spring.ai.dashscope.api-key: ${DASHSCOPE_API_KEY:}
```

DashScope 模式必须存在：

```yaml
spring.ai.dashscope.api-key: ${DASHSCOPE_API_KEY}
```

Windows PowerShell：

```powershell
$env:DASHSCOPE_API_KEY="your-api-key"
```

Windows CMD：

```cmd
set DASHSCOPE_API_KEY=your-api-key
```

Linux / macOS：

```bash
export DASHSCOPE_API_KEY=your-api-key
```

## Agent Runtime 配置

核心配置项：

```yaml
cjlab:
  agent:
    runtime: local-dag
```

当前设计支持：

```text
local-dag
agentscope
```

### `local-dag`

默认运行时。

配置：

```yaml
cjlab.agent.runtime: local-dag
```

对应 Bean：

```java
LocalDagAgentRuntime
```

执行链路：

```text
AgentService
-> AgentRuntime
-> LocalDagAgentRuntime
-> Planner
-> DAG Executor
-> Memory
-> RAG
-> Tool Orchestrator
-> ChatModelGateway
```

适合：

- 可控业务流程
- 固定执行节点
- 易调试、易审计
- 本地开发和默认生产路径

### `agentscope`

AgentScope 运行时预留。

配置：

```yaml
cjlab.agent.runtime: agentscope
```

对应 Bean：

```java
AgentScopeAgentRuntime
```

当前状态：

- 已完成 Runtime 抽象和切换机制
- 已接入 `io.agentscope:agentscope-core`
- 使用 AgentScope `ReActAgent` 执行
- 通过 `ChatModelGateway` 适配 AgentScope `Model`
- 当前内置 `rag_search` AgentScope Tool

执行链路：

```text
AgentService
-> AgentRuntime
-> AgentScopeAgentRuntime
-> ReActAgent
-> AgentScopeChatModelAdapter
-> ChatModelGateway
```

注意：当前实现使用 `agentscope-core`，不是 Spring AI Alibaba 的 `spring-ai-alibaba-starter-agentscope` 自动配置。starter 仍放在 Maven `agentscope` profile 中，供后续开发更深层的 Spring AI Alibaba Agent Framework 集成。

## Maven Profile

### 默认 Maven 构建

默认构建不启用 AgentScope starter。

```bash
mvn clean compile
```

原因：

- `spring-ai-alibaba-starter-agentscope` 依赖链较重
- 当前本地 Maven 仓库曾出现部分依赖 tracking file 写入失败
- AgentScope Runtime 当前仍是预留实现

### `agentscope` Maven profile

如果需要引入 Spring AI Alibaba AgentScope starter：

```bash
mvn clean compile -Pagentscope
```

profile 定义位置：

```text
cjlab-ai-agent-infrastructure/pom.xml
```

内容：

```xml
<profiles>
    <profile>
        <id>agentscope</id>
        <dependencies>
            <dependency>
                <groupId>com.alibaba.cloud.ai</groupId>
                <artifactId>spring-ai-alibaba-starter-agentscope</artifactId>
            </dependency>
        </dependencies>
    </profile>
</profiles>
```

注意：启用 Maven profile 只代表引入依赖，不代表 `cjlab.agent.runtime` 自动切换。

如果同时要切换 Runtime，需要配置：

```yaml
cjlab.agent.runtime: agentscope
```

或启动时覆盖：

```bash
mvn -pl cjlab-ai-agent-server spring-boot:run -Dspring-boot.run.arguments="--cjlab.agent.runtime=agentscope"
```

## 常用启动命令

### 编译全部模块

```bash
mvn clean compile
```

### 安装到本地 Maven 仓库

```bash
mvn install -DskipTests
```

### 启动默认本地模式

```bash
mvn install -DskipTests
mvn -pl cjlab-ai-agent-server spring-boot:run
```

### 启动 DashScope 模式

PowerShell：

```powershell
$env:DASHSCOPE_API_KEY="your-api-key"
mvn -pl cjlab-ai-agent-server spring-boot:run -Dspring-boot.run.profiles=dashscope
```

CMD：

```cmd
set DASHSCOPE_API_KEY=your-api-key
mvn -pl cjlab-ai-agent-server spring-boot:run -Dspring-boot.run.profiles=dashscope
```

Linux / macOS：

```bash
export DASHSCOPE_API_KEY=your-api-key
mvn -pl cjlab-ai-agent-server spring-boot:run -Dspring-boot.run.profiles=dashscope
```

### 使用指定端口启动

```bash
mvn -pl cjlab-ai-agent-server spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"
```

### 使用指定 Runtime 启动

```bash
mvn -pl cjlab-ai-agent-server spring-boot:run -Dspring-boot.run.arguments="--cjlab.agent.runtime=local-dag"
```

### DashScope + 指定 Runtime

```bash
mvn -pl cjlab-ai-agent-server spring-boot:run ^
  -Dspring-boot.run.profiles=dashscope ^
  -Dspring-boot.run.arguments="--cjlab.agent.runtime=local-dag"
```

PowerShell 单行：

```powershell
mvn -pl cjlab-ai-agent-server spring-boot:run -Dspring-boot.run.profiles=dashscope -Dspring-boot.run.arguments="--cjlab.agent.runtime=local-dag"
```

## 配置优先级

Spring Boot 常见配置优先级从高到低：

```text
命令行参数
环境变量
application-{profile}.yml
application.yml
代码默认值
```

例如命令行覆盖端口：

```bash
--server.port=8081
```

会覆盖：

```yaml
server:
  port: 8080
```

## 当前 Bean 装配关系

配置类：

```text
cjlab-ai-agent-server/src/main/java/io/github/cjlab/agent/server/config/AgentConfiguration.java
```

关键 Bean：

```text
ChatModelGateway
ConversationMemory
KnowledgeRepository
KnowledgeRetriever
ToolRegistry
ToolOrchestrator
Planner
DagExecutor
AgentRuntime
AgentService
```

默认装配：

```text
ChatModelGateway              -> local mock
ConversationMemory            -> MyBatisPlusConversationMemory
KnowledgeRepository           -> MyBatisPlusKnowledgeRepository
KnowledgeRetriever            -> Bm25KnowledgeRetriever
ToolRegistry                  -> InMemoryToolRegistry
ToolOrchestrator              -> RuleBasedToolOrchestrator
Planner                       -> DefaultPlanner
DagExecutor                   -> TopologicalDagExecutor
AgentRuntime                  -> LocalDagAgentRuntime
AgentService                  -> DefaultAgentService
```

DashScope profile 下：

```text
ChatModelGateway -> SpringAiChatModelGateway
```

## API 验证

### 聊天

```bash
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d "{\"conversationId\":\"demo\",\"message\":\"hello\"}"
```

### 查询记忆

```bash
curl "http://localhost:8080/api/memory/demo?limit=20"
```

### 写入知识

```bash
curl -X POST http://localhost:8080/api/knowledge \
  -H "Content-Type: application/json" \
  -d "{\"title\":\"Project\",\"content\":\"CJLab AI Agent uses Spring AI Alibaba.\"}"
```

### 检索知识

```bash
curl "http://localhost:8080/api/knowledge/search?query=Spring%20AI&limit=5"
```

### 查询工具

```bash
curl http://localhost:8080/api/tools
```

### 执行工具

```bash
curl -X POST http://localhost:8080/api/tools/current_time/execute \
  -H "Content-Type: application/json" \
  -d "{\"conversationId\":\"demo\",\"input\":\"now\",\"arguments\":{}}"
```

## 常见问题

### 1. 启动时报 DashScope API Key 缺失

错误类似：

```text
DashScope API key must be set
```

原因：

- 启用了 `dashscope` profile
- 但没有配置 `DASHSCOPE_API_KEY`

处理：

```powershell
$env:DASHSCOPE_API_KEY="your-api-key"
```

或者不要启用 `dashscope` profile，使用默认 mock 模式。

### 2. 8080 端口被占用

处理方式 1：改端口。

```bash
mvn -pl cjlab-ai-agent-server spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"
```

处理方式 2：关闭占用进程。

Windows PowerShell：

```powershell
Get-NetTCPConnection -LocalPort 8080
```

### 3. 启用 agentscope runtime 后返回 mock 前缀

默认模式下，`agentscope` runtime 仍然使用本地 mock `ChatModelGateway`，所以响应可能以如下内容开头：

```text
Local mock response: You are CJLab AgentScope Runtime...
```

这是正常现象。要接真实模型，请同时启用 `dashscope` profile 并配置 `DASHSCOPE_API_KEY`，或者启用 `openai-compatible` profile 接入 OpenAI 兼容中转站。

### 4. `-Pagentscope` 构建失败

可能原因：

- AgentScope starter 依赖较多
- 本地 Maven 仓库文件被占用或权限不足
- 镜像仓库部分依赖 POM 写入失败

处理建议：

- 先使用默认构建
- 确认没有其他 Maven/IDE 进程占用本地仓库
- 清理具体失败依赖目录后重试

示例：

```bash
mvn clean compile
```

不要启用：

```bash
-Pagentscope
```

除非正在开发 AgentScope 集成。

### 5. `spring-ai-alibaba-starter-dashscope` 为什么显式写版本

当前父 POM 已导入：

```xml
<artifactId>spring-ai-alibaba-bom</artifactId>
<version>1.1.2.3</version>
```

但此前验证发现该 BOM 没有管理 `spring-ai-alibaba-starter-dashscope` 的版本，所以在 `cjlab-ai-agent-infrastructure/pom.xml` 中显式声明：

```xml
<dependency>
    <groupId>com.alibaba.cloud.ai</groupId>
    <artifactId>spring-ai-alibaba-starter-dashscope</artifactId>
    <version>${spring-ai-alibaba.version}</version>
</dependency>
```

## 推荐配置组合

### 本地开发

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

```bash
mvn -pl cjlab-ai-agent-server spring-boot:run
```

### 接 DashScope 真实模型

```yaml
spring:
  ai:
    model:
      chat: dashscope

cjlab:
  agent:
    runtime: local-dag
```

启动：

```bash
mvn -pl cjlab-ai-agent-server spring-boot:run -Dspring-boot.run.profiles=dashscope
```

### 接 OpenAI 兼容中转站

适用于提供 `/v1/chat/completions` 兼容接口的中转站。

配置文件：

```yaml
spring:
  ai:
    model:
      chat: none

cjlab:
  openai-compatible:
    base-url: ${CJLAB_OPENAI_BASE_URL:https://api.openai.com}
    api-key: ${CJLAB_OPENAI_API_KEY:}
    model: ${CJLAB_OPENAI_MODEL:gpt-4o-mini}
    completions-path: ${CJLAB_OPENAI_CHAT_COMPLETIONS_PATH:/v1/chat/completions}
    temperature: ${CJLAB_OPENAI_TEMPERATURE:0.7}
    timeout: ${CJLAB_OPENAI_TIMEOUT:60s}
```

该模式使用项目内置的 `OpenAiCompatibleChatModelGateway`，不依赖 Spring AI OpenAI starter。原因是当前项目使用 Spring Boot 4.1 / Spring Framework 7，而 Spring AI OpenAI 1.1.2 与该组合存在 `HttpHeaders.addAll(...)` 方法签名兼容问题。

Windows PowerShell：

```powershell
$env:CJLAB_OPENAI_BASE_URL="https://your-relay.example.com"
$env:CJLAB_OPENAI_API_KEY="your-relay-api-key"
$env:CJLAB_OPENAI_MODEL="gpt-4o-mini"
mvn -pl cjlab-ai-agent-server spring-boot:run -Dspring-boot.run.profiles=openai-compatible
```

如果中转站要求完整路径不是 `/v1/chat/completions`：

```powershell
$env:CJLAB_OPENAI_CHAT_COMPLETIONS_PATH="/v1/chat/completions"
```

JAR 启动：

```powershell
java -jar cjlab-ai-agent-server\target\cjlab-ai-agent-server-1.0-SNAPSHOT.jar `
  --spring.profiles.active=openai-compatible `
  --cjlab.openai-compatible.base-url=https://your-relay.example.com `
  --cjlab.openai-compatible.api-key=your-relay-api-key `
  --cjlab.openai-compatible.model=gpt-4o-mini
```

AgentScope runtime 同样可以复用中转站模型：

```powershell
mvn -pl cjlab-ai-agent-server spring-boot:run `
  -Dspring-boot.run.profiles=openai-compatible `
  -Dspring-boot.run.arguments="--cjlab.agent.runtime=agentscope"
```

### 开发 AgentScope 集成

默认项目已经可以使用 `cjlab.agent.runtime=agentscope` 启动 `agentscope-core` 版 Runtime。

启动：

```bash
mvn -pl cjlab-ai-agent-server spring-boot:run -Dspring-boot.run.arguments="--cjlab.agent.runtime=agentscope"
```

如果要进一步开发 Spring AI Alibaba AgentScope starter 集成，再启用 Maven profile：

```bash
mvn clean compile -Pagentscope
```

配置：

```yaml
cjlab:
  agent:
    runtime: agentscope
```
