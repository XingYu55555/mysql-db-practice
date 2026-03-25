# SQL Judge Engine 文档更新总结

本文档记录了 SQL Judge Engine 项目文档与实际代码实现对比后的更新内容。

## 更新日期
2026-03-25

## 更新概述

本次更新确保所有项目文档与实际代码实现保持一致，主要涉及以下方面：
1. 技术栈版本信息补充
2. 数据库 Schema 字段补充
3. AI 判题配置更新
4. 判题结果状态扩展
5. 超时与重试策略细化

---

## 详细变更列表

### 1. architecture/microservices.md

#### 变更内容
- **技术栈表格**: 添加版本号列，明确指定各组件版本
  - Spring Boot: 3.2.3
  - Java: 17
  - Spring Cloud: 2023.0.0
  - OpenAPI: 3.0.3
- **Gateway 技术栈**: 从 "Spring Cloud Gateway" 更正为 "Nginx"
- **AI 服务**: 添加阿里云百炼 (OpenAI 兼容)

#### 文件路径
`/home/xingyu/projects/mysql-db-practice/sql-judge-engine/docs/architecture/microservices.md`

---

### 2. architecture/ai-judge.md

#### 变更内容
- **判题结果状态**: 扩展状态枚举
  - 新增: `AI_LOW_CONFIDENCE` (AI置信度过低)
  - 新增: `AI_FAILED` (AI服务调用失败)
  - 新增: `AI_TIMEOUT` (AI服务超时)
- **配置项**: 更新为实际配置项名称
  - `app.ai.enabled` (默认: true)
  - `app.ai.timeout` (默认: 30000ms)
  - `app.ai.retry` (默认: 2)
  - `app.ai.confidence-threshold.high` (默认: 0.9)
  - `app.ai.confidence-threshold.low` (默认: 0.6)
  - `app.ai.api-key`
  - `app.ai.api-host`
  - `app.ai.model` (默认: qwen-turbo)
- **Docker Compose 环境变量**: 添加配置示例

#### 文件路径
`/home/xingyu/projects/mysql-db-practice/sql-judge-engine/docs/architecture/ai-judge.md`

---

### 3. architecture/data-flow.md

#### 变更内容
- **核心判题流程时序图**: 
  - 添加 AI Service (阿里云百炼) 参与者
  - 更新容器获取返回信息（使用 connectionToken 替代明文密码）
  - 细化 DDL/DCL 判题流程（区分 aiAssisted 标志）
  - 添加 ACK 确认说明

#### 文件路径
`/home/xingyu/projects/mysql-db-practice/sql-judge-engine/docs/architecture/data-flow.md`

---

### 4. architecture/judge-container.md

#### 变更内容
- **acquire 流程**: 
  - 添加 `containerName` 返回字段
  - 添加 `status` 返回字段
  - 添加空 initSql 检查逻辑
  - 添加实际实现说明（HTTP 接口、Token 有效期等）

#### 文件路径
`/home/xingyu/projects/mysql-db-practice/sql-judge-engine/docs/architecture/judge-container.md`

---

### 5. architecture/timeout-retry.md

#### 变更内容
- **AI 服务超时**: 添加置信度阈值说明
  - 高置信度阈值: 0.9
  - 低置信度阈值: 0.6
- **HTTP 调用重试**: 更新表格格式，添加最大重试次数列
- **实际代码实现**: 添加 JudgeServiceImpl.java 中的重试代码示例

#### 文件路径
`/home/xingyu/projects/mysql-db-practice/sql-judge-engine/docs/architecture/timeout-retry.md`

---

### 6. schema/schema.md

#### 变更内容
- **problems 表**: 添加缺失字段
  - `status` ENUM('DRAFT', 'READY', 'PUBLISHED', 'ARCHIVED')
  - `ai_assisted` BOOLEAN DEFAULT FALSE
  - `expected_result` TEXT (期望结果JSON)
  - `expected_type` ENUM('RESULT_SET', 'DATA_SNAPSHOT', 'METADATA', 'PRIVILEGE')
- **judge_results 表**: 
  - 移除 `submission_id` 的 UNIQUE 约束（支持多测试用例）
  - 扩展 `status` 枚举: 添加 `AI_APPROVED`, `AI_REJECTED`
- **数据字典**: 
  - 添加 `problems.expected_type` 枚举值
  - 添加 `containers.status` 枚举值

#### 文件路径
`/home/xingyu/projects/mysql-db-practice/sql-judge-engine/docs/schema/schema.md`

---

### 7. configs/mysql/init.sql

#### 变更内容
- **problems 表**: 添加缺失字段
  - `status` ENUM('DRAFT', 'READY', 'PUBLISHED', 'ARCHIVED') DEFAULT 'DRAFT'
  - `ai_assisted` BOOLEAN DEFAULT FALSE
  - `expected_result` TEXT
  - `expected_type` ENUM('RESULT_SET', 'DATA_SNAPSHOT', 'METADATA', 'PRIVILEGE')
  - `idx_status` 索引
- **judge_results 表**:
  - 移除 `submission_id` 的 UNIQUE 约束
  - 扩展 `status` 枚举: 添加 `AI_APPROVED`, `AI_REJECTED`

#### 文件路径
`/home/xingyu/projects/mysql-db-practice/sql-judge-engine/configs/mysql/init.sql`

---

### 8. testing/integration-test-architecture.md

#### 变更内容
- **judge-service 核心组件**: 添加 `AiJudgeService`
- **依赖服务**: 添加 AI 服务 (阿里云百炼)
- **JudgeResult**: 添加状态说明注释

#### 文件路径
`/home/xingyu/projects/mysql-db-practice/sql-judge-engine/docs/testing/integration-test-architecture.md`

---

## 验证清单

- [x] 所有文档已与真实实现对比
- [x] 过时的信息已更新
- [x] 缺失的内容已补充
- [x] 文档格式统一

## 后续建议

1. **定期同步**: 建议每次代码变更后同步更新相关文档
2. **版本控制**: 考虑为文档添加版本号，与代码版本对应
3. **自动化检查**: 可建立 CI 流程检查文档与代码的一致性

---

## 参考文件

### 核心代码文件
- `/home/xingyu/projects/mysql-db-practice/sql-judge-engine/pom.xml` - 技术栈版本定义
- `/home/xingyu/projects/mysql-db-practice/sql-judge-engine/services/judge-service/src/main/java/com/sqljudge/judgeservice/service/impl/JudgeServiceImpl.java` - 判题逻辑实现
- `/home/xingyu/projects/mysql-db-practice/sql-judge-engine/services/judge-service/src/main/java/com/sqljudge/judgeservice/service/impl/AiJudgeServiceImpl.java` - AI 判题实现
- `/home/xingyu/projects/mysql-db-practice/sql-judge-engine/services/judge-service/src/main/resources/application.yml` - 配置文件
- `/home/xingyu/projects/mysql-db-practice/sql-judge-engine/docker-compose.yml` - 部署配置
- `/home/xingyu/projects/mysql-db-practice/sql-judge-engine/configs/gateway/nginx.conf` - 网关配置

### 更新后的文档
- `/home/xingyu/projects/mysql-db-practice/sql-judge-engine/docs/architecture/microservices.md`
- `/home/xingyu/projects/mysql-db-practice/sql-judge-engine/docs/architecture/ai-judge.md`
- `/home/xingyu/projects/mysql-db-practice/sql-judge-engine/docs/architecture/data-flow.md`
- `/home/xingyu/projects/mysql-db-practice/sql-judge-engine/docs/architecture/judge-container.md`
- `/home/xingyu/projects/mysql-db-practice/sql-judge-engine/docs/architecture/timeout-retry.md`
- `/home/xingyu/projects/mysql-db-practice/sql-judge-engine/docs/schema/schema.md`
- `/home/xingyu/projects/mysql-db-practice/sql-judge-engine/docs/testing/integration-test-architecture.md`
