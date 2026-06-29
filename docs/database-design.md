# 数据库设计约定

项目使用 `cjlab-boot-starter-mybatis-plus` 作为 MyBatis-Plus 基础能力来源。

## DO 基类

新增持久化实体默认继承：

```java
io.cjlab.mybatisplus.datasource.core.BaseDO
```

`BaseDO` 提供统一公共字段：

- `createTime`
- `updateTime`
- `creator`
- `updater`
- `deleted`

对应数据库列统一使用：

```text
create_time
update_time
creator
updater
deleted
```

实体类只声明业务字段和主键字段，不重复声明创建时间、更新时间、创建人、更新人和逻辑删除字段。

## 表设计原则

- 主键字段按业务需要声明在实体类中，并使用 `@TableId` 标注。
- 公共审计字段统一来自 `BaseDO`。
- 查询排序使用 `createTime` / `updateTime` 的 getter，例如 `Entity::getCreateTime`。
- SQL 脚本必须和 `BaseDO` 字段保持一致，避免使用 `created_at`、`updated_at` 这类非项目标准列名。
- 逻辑删除统一使用 `deleted`，默认值为 `0`。
