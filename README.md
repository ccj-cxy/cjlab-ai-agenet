# cjlab-ai-agent

`cjlab-ai-agent` 是一个基于 Java 21、Spring Boot 4.1、Spring AI Alibaba、MyBatis-Plus 和 MySQL 的 AI Agent 工程。项目按能力拆成多模块，已经包含用户登录、SSE 对话、会话记忆、会话摘要、角色卡、RAG 知识库、工具调用、网页检索工具、执行日志和前端验证台。

## 当前能力

- 邮箱注册、邮箱密码登录、Bearer Token 登录态。
- 当前登录人隔离会话、知识库、工具调用和角色卡数据。
- SSE 对话页面，支持模型 `<think>...</think>` 思考内容折叠展示。
- 角色卡管理页：维护角色名称、描述、提示词和头像。
- 对话页角色卡选择：选择不同角色卡后影响模型回答风格。
- 角色卡持久化到 MySQL，实体继承 `cjlab-boot-starter-mybatis-plus` 的 `BaseDO`。
- 会话消息持久化到 MySQL，并支持会话摘要读取。
- RAG 知识库持久化到 MySQL，当前检索实现为 BM25 关键词检索。
- 工具系统：当前时间工具、网页检索工具、工具调用记录持久化。
- 后端控制台执行日志：聊天、SSE、角色卡、工具执行都会输出关键执行日志。
- 前端控制台请求日志：对话页和角色卡管理页都会在浏览器 DevTools 输出脱敏请求日志。

## 技术栈

- Java 21
- Maven 多模块
- Spring Boot 4.1.0
- Spring AI Alibaba 1.1.2.3
- MySQL
- MyBatis-Plus
- `io.github.ccj-cxy:cjlab-boot-starter-mybatis-plus`
- DashScope profile
- OpenAI-compatible HTTP 网关 profile
- AgentScope Runtime 预留实现

## 模块结构

```text
cjlab-ai-agent
  cjlab-ai-agent-common          通用异常和公共能力
  cjlab-ai-agent-memory          会话消息、会话摘要、Memory 持久化
  cjlab-ai-agent-tool            工具接口、工具注册、工具调用记录、网页检索工具
  cjlab-ai-agent-rag             知识库、BM25 检索、知识库持久化
  cjlab-ai-agent-user            用户、会话 token、角色卡持久化
  cjlab-ai-agent-core            AgentService、Runtime、Planner、DAG、Chat 模型抽象
  cjlab-ai-agent-infrastructure  模型网关、OpenAI-compatible、DashScope、AgentScope 适配
  cjlab-ai-agent-server          Spring Boot 启动、API、静态前端页面
```

## 页面入口

启动后访问：

```text
http://localhost:8080/
```

主要页面：

- `/`：前端对话页。登录后可选择角色卡并发起 SSE 对话。
- `/role-cards.html`：角色卡管理页。登录后可新增、复制、删除、保存角色卡和头像。

对话页和管理页共用同一套后端角色卡接口，角色卡数据按当前登录人隔离保存。

## 数据库

项目默认使用 MySQL：

```yaml
spring:
  datasource:
    url: ${CJLAB_MYSQL_URL:jdbc:mysql://localhost:3306/cjlab_ai_agent?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai}
    username: ${CJLAB_MYSQL_USERNAME:root}
    password: ${CJLAB_MYSQL_PASSWORD:123456}
```

启动时会执行：

```text
cjlab-ai-agent-server/src/main/resources/schema.sql
```

并通过：

```yaml
spring:
  sql:
    init:
      mode: always
      continue-on-error: true
```

自动创建或补齐本地开发所需表结构。完整 SQL 文档见：

```text
docs/sql/user-schema.sql
```

新增 DO 默认继承：

```java
io.cjlab.mybatisplus.datasource.core.BaseDO
```

公共字段统一使用：

```text
create_time
update_time
creator
updater
deleted
```

## 运行前准备

1. 安装 JDK 21 和 Maven 3.9+。
2. 启动 MySQL，并确认存在数据库：

```sql
CREATE DATABASE IF NOT EXISTS cjlab_ai_agent
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;
```

3. 按需配置环境变量：

```powershell
$env:CJLAB_MYSQL_URL="jdbc:mysql://localhost:3306/cjlab_ai_agent?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai"
$env:CJLAB_MYSQL_USERNAME="root"
$env:CJLAB_MYSQL_PASSWORD="123456"
```

## 启动项目

当前 `application.yml` 默认激活：

```yaml
spring:
  profiles:
    active: openai-compatible
```

也就是说默认会使用项目内置的 OpenAI-compatible HTTP 网关。建议通过环境变量显式配置中转站信息：

```powershell
$env:CJLAB_OPENAI_BASE_URL="https://your-relay.example.com"
$env:CJLAB_OPENAI_API_KEY="your-api-key"
$env:CJLAB_OPENAI_MODEL="your-model"
$env:CJLAB_OPENAI_CHAT_COMPLETIONS_PATH="/v1/chat/completions"
```

启动：

```powershell
mvn -pl cjlab-ai-agent-server -am install -DskipTests
mvn -pl cjlab-ai-agent-server spring-boot:run
```

指定端口：

```powershell
mvn -pl cjlab-ai-agent-server spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"
```

## 运行模式

### OpenAI-compatible

默认 profile。配置前缀：

```yaml
cjlab:
  openai-compatible:
    base-url: ${CJLAB_OPENAI_BASE_URL}
    api-key: ${CJLAB_OPENAI_API_KEY}
    model: ${CJLAB_OPENAI_MODEL}
    completions-path: ${CJLAB_OPENAI_CHAT_COMPLETIONS_PATH:/v1/chat/completions}
    temperature: ${CJLAB_OPENAI_TEMPERATURE:0.7}
    timeout: ${CJLAB_OPENAI_TIMEOUT:60s}
```

启动：

```powershell
mvn -pl cjlab-ai-agent-server spring-boot:run -Dspring-boot.run.profiles=openai-compatible
```

### DashScope

配置：

```powershell
$env:DASHSCOPE_API_KEY="your-api-key"
```

启动：

```powershell
mvn -pl cjlab-ai-agent-server spring-boot:run -Dspring-boot.run.profiles=dashscope
```

### Local Mock

如果只想验证本地链路，不接真实模型，可以临时覆盖 profile 和模型网关：

```powershell
mvn -pl cjlab-ai-agent-server spring-boot:run -Dspring-boot.run.profiles=default
```

或在启动参数中覆盖配置，确保不激活 `openai-compatible` / `dashscope`。

### Agent Runtime

默认：

```yaml
cjlab:
  agent:
    runtime: local-dag
```

可选值：

- `local-dag`：默认本地 DAG Runtime。
- `agentscope`：AgentScope Runtime 预留实现。

启动时切换：

```powershell
mvn -pl cjlab-ai-agent-server spring-boot:run -Dspring-boot.run.arguments="--cjlab.agent.runtime=agentscope"
```

## API 概览

多数业务接口需要登录后携带：

```http
Authorization: Bearer <accessToken>
```

### 用户

注册：

```bash
curl -X POST http://localhost:8080/api/users/register \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"demo@example.com\",\"password\":\"password123\",\"displayName\":\"Demo\"}"
```

登录：

```bash
curl -X POST http://localhost:8080/api/users/login \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"demo@example.com\",\"password\":\"password123\"}"
```

当前用户：

```bash
curl http://localhost:8080/api/users/me \
  -H "Authorization: Bearer <accessToken>"
```

### 聊天

非流式：

```bash
curl -X POST http://localhost:8080/api/chat \
  -H "Authorization: Bearer <accessToken>" \
  -H "Content-Type: application/json" \
  -d "{\"conversationId\":\"demo\",\"message\":\"hello\",\"roleCard\":{\"id\":\"default\",\"name\":\"默认助手\",\"description\":\"平衡、直接、可执行\",\"instruction\":\"简洁回答\"}}"
```

SSE 流式：

```bash
curl -N -X POST http://localhost:8080/api/chat/stream \
  -H "Authorization: Bearer <accessToken>" \
  -H "Accept: text/event-stream" \
  -H "Content-Type: application/json" \
  -d "{\"conversationId\":\"demo\",\"message\":\"hello\"}"
```

### 角色卡

查询当前用户角色卡：

```bash
curl http://localhost:8080/api/role-cards \
  -H "Authorization: Bearer <accessToken>"
```

保存角色卡：

```bash
curl -X POST http://localhost:8080/api/role-cards \
  -H "Authorization: Bearer <accessToken>" \
  -H "Content-Type: application/json" \
  -d "{\"id\":\"engineer\",\"name\":\"后端工程师\",\"description\":\"Java/Spring/MyBatis\",\"instruction\":\"以资深后端工程师风格回答\",\"avatar\":\"\"}"
```

写入默认角色卡：

```bash
curl -X POST http://localhost:8080/api/role-cards/defaults \
  -H "Authorization: Bearer <accessToken>"
```

删除角色卡：

```bash
curl -X DELETE http://localhost:8080/api/role-cards/engineer \
  -H "Authorization: Bearer <accessToken>"
```

### 会话记忆和摘要

读取最近消息：

```bash
curl "http://localhost:8080/api/memory/demo?limit=20" \
  -H "Authorization: Bearer <accessToken>"
```

读取会话摘要：

```bash
curl http://localhost:8080/api/memory/demo/summary \
  -H "Authorization: Bearer <accessToken>"
```

### 知识库

写入知识文档：

```bash
curl -X POST http://localhost:8080/api/knowledge \
  -H "Authorization: Bearer <accessToken>" \
  -H "Content-Type: application/json" \
  -d "{\"title\":\"Project\",\"content\":\"CJLab AI Agent uses Spring Boot and MyBatis-Plus.\",\"metadata\":{\"source\":\"readme\"}}"
```

查询列表：

```bash
curl http://localhost:8080/api/knowledge \
  -H "Authorization: Bearer <accessToken>"
```

检索：

```bash
curl "http://localhost:8080/api/knowledge/search?query=Spring%20Boot&limit=5" \
  -H "Authorization: Bearer <accessToken>"
```

### 工具

工具列表：

```bash
curl http://localhost:8080/api/tools \
  -H "Authorization: Bearer <accessToken>"
```

执行当前时间工具：

```bash
curl -X POST http://localhost:8080/api/tools/current_time/execute \
  -H "Authorization: Bearer <accessToken>" \
  -H "Content-Type: application/json" \
  -d "{\"conversationId\":\"demo\",\"input\":\"now\",\"arguments\":{}}"
```

执行网页检索工具：

```bash
curl -X POST http://localhost:8080/api/tools/web_search/execute \
  -H "Authorization: Bearer <accessToken>" \
  -H "Content-Type: application/json" \
  -d "{\"conversationId\":\"demo\",\"input\":\"CJLab AI Agent\",\"arguments\":{\"query\":\"CJLab AI Agent\",\"limit\":5}}"
```

查询工具调用记录：

```bash
curl "http://localhost:8080/api/tools/calls?conversationId=demo&limit=20" \
  -H "Authorization: Bearer <accessToken>"
```

## 执行日志

后端控制台会输出关键执行日志：

```text
role_cards.list
role_cards.save
role_cards.delete
chat.execute_start
chat.execute_done
chat.stream_start
chat.stream_done
tools.execute_start
tools.execute_done
```

日志包含用户 ID、会话 ID、角色 ID、工具名、耗时和输入输出长度。不会打印密码、token 和完整模型回复。

前端页面也会在浏览器 DevTools 控制台输出：

```text
[cjlab-agent] ...
```

登录和注册请求会对 `password`、`accessToken` 做脱敏处理。

## 核心调用链

```text
Browser
-> ChatController
-> DefaultAgentService
-> ConversationMemory 写入 USER
-> AgentRuntime
-> Planner
-> DAG: Memory / Retrieval / Tool / Generation
-> ChatModelGateway
-> ConversationMemory 写入 ASSISTANT
-> ConversationSummaryRepository 按条件更新摘要
-> SSE / JSON 返回给前端
```

角色卡在对话请求中作为 `ChatRoleCard` 传入 Runtime，用于拼入系统提示词，控制模型回答风格。

## 常用命令

全量编译：

```bash
mvn clean compile
```

只编译 server 及依赖：

```bash
mvn -pl cjlab-ai-agent-server -am compile
```

安装到本地 Maven 仓库：

```bash
mvn -pl cjlab-ai-agent-server -am install -DskipTests
```

启动 server：

```bash
mvn -pl cjlab-ai-agent-server spring-boot:run
```

指定 8081 端口：

```bash
mvn -pl cjlab-ai-agent-server spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"
```

前端语法检查：

```bash
node --check cjlab-ai-agent-server/src/main/resources/static/app.js
node --check cjlab-ai-agent-server/src/main/resources/static/role-cards.js
```

## 相关文档

- [docs/project-architecture.md](docs/project-architecture.md)
- [docs/user-management.md](docs/user-management.md)
- [docs/database-design.md](docs/database-design.md)
- [docs/configuration-guide.md](docs/configuration-guide.md)
- [docs/sql/user-schema.sql](docs/sql/user-schema.sql)

## 当前边界

- SSE 当前是应用层流式输出，模型网关仍以完整响应为主。
- RAG 当前是 BM25 关键词检索，不是向量检索。
- 角色卡头像支持 URL 和 data URL，生产环境建议改为对象存储，只在数据库保存图片地址。
- `schema.sql` 适合本地开发自动建表；生产环境建议使用 Flyway 或 Liquibase 管理迁移。
- 网页检索工具适合验证工具链路，生产环境需要更完整的限流、审计、搜索源治理和 SSRF 防护策略。
