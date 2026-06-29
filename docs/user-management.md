# 用户管理模块说明

本文档说明 `cjlab-ai-agent-user` 子工程的设计、接口、MySQL/MyBatis-Plus 实现和数据库建表语句。

## 1. 能力范围

当前用户管理模块提供：

- 邮箱注册
- 邮箱密码登录
- PBKDF2 密码哈希
- Bearer Token 登录态
- 查询当前登录用户
- MySQL 建表语句

用户账号和登录态固定使用 MySQL 持久化，通过项目 starter 中的 `cjlab-boot-starter-mybatis-plus` 接入 MyBatis-Plus 和 MySQL。

## 2. 模块位置

```text
cjlab-ai-agent-user
  UserAccount
  UserStatus
  UserRepository
  persistence/entity/UserAccountEntity
  persistence/mapper/UserAccountMapper
  repository/mybatis/MyBatisPlusUserRepository
  PasswordHasher
  Pbkdf2PasswordHasher
  UserSession
  UserSessionService
  persistence/entity/UserSessionEntity
  persistence/mapper/UserSessionMapper
  repository/mybatis/MyBatisPlusUserSessionService
  UserService
  RegisterUserRequest
  LoginRequest
  LoginResponse
  UserProfileResponse
```

Server API：

```text
cjlab-ai-agent-server
  UserController
```

SQL：

```text
docs/sql/user-schema.sql
```

## 3. 类职责

### `UserAccount`

用户账号领域模型。包含用户 ID、邮箱、显示名、密码哈希、状态、创建时间和更新时间。

### `UserStatus`

用户状态枚举：

- `ACTIVE`：正常。
- `DISABLED`：禁用。

### `UserRepository`

用户仓库接口，定义 `save`、`findById`、`findByEmail`、`existsByEmail`。

### `MyBatisPlusUserRepository`

MySQL 用户仓库，基于 MyBatis-Plus `BaseMapper` 读写 `cjlab_user_account`。

方法映射：

- `save` -> `insertOrUpdate`
- `findById` -> 按主键查询
- `findByEmail` -> 按邮箱唯一索引查询
- `existsByEmail` -> 邮箱唯一性检查

### `UserAccountEntity`

MyBatis-Plus 持久化实体，对应表 `cjlab_user_account`。负责把数据库字段映射为 Java 字段。

### `UserAccountMapper`

MyBatis-Plus Mapper，继承 `BaseMapper<UserAccountEntity>`。仓库实现通过它完成账号表的增删改查。

### `PasswordHasher`

密码哈希接口，定义 `hash` 和 `matches`。

### `Pbkdf2PasswordHasher`

PBKDF2 密码哈希实现，不保存明文密码。哈希格式：

```text
pbkdf2_sha256$iterations$salt$hash
```

默认参数：

- 算法：`PBKDF2WithHmacSHA256`
- 迭代次数：`120000`
- salt：16 bytes
- key：256 bits

### `UserSession`

登录态模型，包含 token、userId、createdAt、expiresAt。

### `UserSessionService`

登录态服务接口，定义 `create`、`findByToken`、`ttl`。

### `MyBatisPlusUserSessionService`

MySQL 登录态服务，基于 MyBatis-Plus `BaseMapper` 读写 `cjlab_user_session`。

能力：

- 登录时写入 token。
- 查询 token 时检查是否过期。
- 过期 token 会被删除。
- 提供 `deleteExpired` 便于后续加定时清理任务。

### `UserSessionEntity`

MyBatis-Plus 持久化实体，对应表 `cjlab_user_session`。

### `UserSessionMapper`

MyBatis-Plus Mapper，继承 `BaseMapper<UserSessionEntity>`。登录态服务通过它保存 token、查询 token 和删除过期 token。

### `UserService`

用户业务服务，负责注册、登录、查询当前用户。

注册流程：

```text
normalize email
-> validate email
-> validate password
-> check duplicate email
-> hash password
-> save user
-> return profile
```

登录流程：

```text
normalize email
-> find user
-> check ACTIVE
-> verify password
-> create session token
-> return token + profile
```

### `UserController`

用户 HTTP API：

```text
POST /api/users/register
POST /api/users/login
GET /api/users/me
```

## 4. API 示例

### 注册

```http
POST /api/users/register
Content-Type: application/json
```

```json
{
  "email": "test@example.com",
  "password": "password123",
  "displayName": "Test User"
}
```

### 登录

```http
POST /api/users/login
Content-Type: application/json
```

```json
{
  "email": "test@example.com",
  "password": "password123"
}
```

响应：

```json
{
  "accessToken": "...",
  "expiresAt": "...",
  "user": {
    "id": "...",
    "email": "test@example.com",
    "displayName": "Test User",
    "status": "ACTIVE",
    "createdAt": "..."
  }
}
```

### 当前用户

```http
GET /api/users/me
Authorization: Bearer <accessToken>
```

## 5. 配置

数据库和 session TTL：

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/cjlab_ai_agent?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password:

cjlab:
  user:
    session-ttl: 24h
```

推荐用环境变量：

```powershell
$env:CJLAB_MYSQL_URL="jdbc:mysql://localhost:3306/cjlab_ai_agent?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai"
$env:CJLAB_MYSQL_USERNAME="root"
$env:CJLAB_MYSQL_PASSWORD="your-password"
java -jar cjlab-ai-agent-server\target\cjlab-ai-agent-server-1.0-SNAPSHOT.jar
```

## 6. 建表语句

建表 SQL 位于：

```text
docs/sql/user-schema.sql
```

包含：

- `cjlab_user_account`
- `cjlab_user_session`

执行方式：

```powershell
mysql -uroot -p < docs/sql/user-schema.sql
```

## 7. 生产化建议

- 已提供 MySQL/MyBatis-Plus 实现，生产环境建议继续补充迁移工具，例如 Flyway 或 Liquibase。
- `UserSessionService` 如需高并发和分布式部署，可以改成 Redis 实现。
- 增加邮箱验证码。
- 增加忘记密码和重置密码。
- 增加登录失败次数限制。
- 增加 Refresh Token。
- 增加接口鉴权过滤器。
- 给 Chat、Knowledge、Memory 增加 `userId` 隔离。
