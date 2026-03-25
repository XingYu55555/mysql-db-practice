# SQL 判题引擎 - 超时与重试策略

## 1. 概述

本文档定义 SQL 判题引擎系统中各组件的超时配置和重试策略，确保系统在高负载或部分故障时仍能稳定运行。

---

## 2. 超时配置

### 2.1 HTTP 请求超时

| 服务 | 接口 | 超时时间 | 说明 |
|------|------|----------|------|
| user-service | 所有接口 | 30s | 用户操作响应 |
| problem-service | 所有接口 | 30s | 题目查询/创建 |
| submission-service | POST /submission | 10s | 提交接口快速响应 |
| result-service | 查询接口 | 30s | 成绩查询 |
| container-manager | acquire | 10s | 默认获取超时 |
| container-manager | acquire | 5s | 简单题超时 |
| container-manager | acquire | 15s | 难题超时 |

### 2.2 数据库超时

| 数据库 | 配置项 | 超时时间 | 说明 |
|--------|--------|----------|------|
| MySQL (业务库) | `connectTimeout` | 10s | 连接建立超时 |
| MySQL (业务库) | `socketTimeout` | 30s | 查询执行超时 |
| MySQL (容器内) | `MAX_EXECUTION_TIME` | 30s | 单条 SQL 最大执行时间 |
| MySQL (容器内) | `MAX_EXECUTION_TIME` | 10s | 简单题 DQL |
| MySQL (容器内) | `MAX_EXECUTION_TIME` | 60s | 复杂 DDL/DML |

### 2.3 RabbitMQ 超时

| 配置项 | 超时时间 | 说明 |
|--------|----------|------|
| Connection timeout | 10s | 建立连接超时 |
| Channel timeout | 30s | 信道操作超时 |
| Message publish timeout | 5s | 消息发布超时 |
| Message consume timeout | 60s | 消息消费最大等待 |

### 2.4 容器相关超时

| 配置项 | 超时时间 | 说明 |
|--------|----------|------|
| 容器启动等待 | 30s | 容器内 MySQL 就绪等待 |
| 容器健康检查 | 5s | 单次健康检查超时 |
| 容器获取等待 | 10s | 从池中获取容器 |
| 判题总超时 | 120s | 单次判题最大耗时 |

### 2.5 AI 服务超时

| 配置项 | 超时时间 | 说明 |
|--------|----------|------|
| AI API 调用 | 30s | DDL/DCL 语义分析超时 |
| AI 重试次数 | 2次 | 超时后重试 |
| AI 高置信度阈值 | 0.9 | 高于此值直接采用 AI 判定 |
| AI 低置信度阈值 | 0.6 | 低于此值使用兜底策略 |

---

## 3. 重试策略

### 3.1 RabbitMQ 消息投递重试

```yaml
# 判题任务投递
exchange: judge-exchange
routing-key: judge.task
retry:
  max-attempts: 3
  initial-interval: 1s
  multiplier: 2
  max-interval: 10s

# 死信队列
dlx: judge-dlx
dlq: judge-tasks-dlq
```

| 重试次数 | 延迟 | 累计延迟 |
|----------|------|----------|
| 第1次重试 | 1s | 1s |
| 第2次重试 | 2s | 3s |
| 第3次重试 | 4s | 7s |
| 失败后 | - | 进入死信队列（judge-tasks-dlq） |

**手动 ACK**：判题流程全部完成后才发送 ACK，包括容器执行、结果比对、结果回写。

### 3.2 HTTP 调用重试

| 调用方 | 被调用方 | 重试策略 | 最大重试次数 |
|--------|----------|----------|-------------|
| submission-service | problem-service | 不重试（读操作） | 0 |
| submission-service | RabbitMQ | 最多3次，指数退避 | 3 |
| judge-service | problem-service | 最多3次，指数退避 | 3 |
| judge-service | container-manager | 最多3次，指数退避 | 3 |
| judge-service | result-service | 最多3次，指数退避 | 3 |
| judge-service | AI 服务 | 最多2次，固定间隔1s | 2 |

**实际代码实现** (JudgeServiceImpl.java):
```java
private static final int MAX_RETRIES = 2;  // 最大重试2次，共3次尝试

// 指数退避策略
long delay = baseDelay * (long) Math.pow(2, attempt - 1);
Thread.sleep(delay);
```

### 3.3 容器获取重试

```java
// 容器获取重试逻辑
public ContainerInfo acquireContainer(int problemDifficulty) {
    int maxAttempts = 3;
    long baseDelay = 1000; // 1s

    for (int attempt = 1; attempt <= maxAttempts; attempt++) {
        try {
            return containerManagerClient.acquire();
        } catch (ContainerUnavailableException e) {
            if (attempt == maxAttempts) {
                throw new ContainerPoolExhaustedException();
            }
            long delay = baseDelay * (long) Math.pow(2, attempt - 1);
            Thread.sleep(delay);
        }
    }
}
```

---

## 4. 判题任务超时配置

### 4.1 按题目难度分级

| 难度 | 容器 Token 有效期 | SQL 执行超时 | 判题总超时 |
|------|-------------------|--------------|------------|
| EASY | 10 分钟 | 10s | 30s |
| MEDIUM | 30 分钟 | 30s | 60s |
| HARD | 60 分钟 | 60s | 120s |

### 4.2 判题流程各阶段超时

```
学生提交 SQL
    ↓
submission-service 记录 (≤2s)
    ↓
投递 RabbitMQ (≤5s)
    ↓
judge-service 消费 (≤5s)
    ↓
申请容器 (≤10s)
    ↓
执行初始化 SQL (≤10s)
    ↓
执行学生 SQL (≤难度超时)
    ↓
结果比对 (≤5s)
    ↓
回写结果 (≤5s)
    ↓
释放容器 (≤5s)
    ↓
总计 ≤ 难度超时 + 60s
```

---

## 5. 生产环境优化项

> **📌 设计意图记录**
>
> 以下两项为生产环境优化项，当前开发环境保留简单重试机制即可。
> 记录设计意图但不强制现在就实现。

### 5.1 熔断机制（生产环境）

当前使用简单重试（最多3次）处理下游服务调用失败。

**生产环境建议**：引入 Resilience4j 或 Sentinel 熔断器，避免雪崩效应。

| 场景 | 当前处理 | 生产环境建议 |
|------|----------|--------------|
| container-manager 不可用 | 重试3次后抛 ContainerPoolExhaustedException | 熔断器打开，快速失败 |
| result-service 不可用 | 重试3次后消息重新入队 | 熔断器打开，防止线程池耗尽 |

---

## 6. 错误处理策略

### 5.1 超时错误处理

| 场景 | 处理方式 |
|------|----------|
| SQL 执行超时 | 返回 TIME_LIMIT 状态，记录超时信息 |
| 容器获取超时 | 重试3次后返回 503，学生端提示"判题服务繁忙" |
| RabbitMQ 投递超时 | 重试3次后返回 503，学生端提示"提交失败" |
| AI 服务超时 | 使用规则引擎兜底，不阻塞判题 |

### 5.2 失败错误码

| 错误码 | 含义 | HTTP 状态码 |
|--------|------|-------------|
| 1001 | 用户名已存在 | 409 |
| 1002 | 密码错误 | 401 |
| 1003 | 账号被禁用 | 403 |
| 2001 | 题目不存在 | 404 |
| 3001 | 提交不存在 | 404 |
| 3002 | 判题服务不可用 | 503 |
| 4001 | 容器池耗尽 | 503 |
| 5001 | 内部错误 | 500 |

---

## 7. 配置示例

### 6.1 Spring Boot 配置

```yaml
# application.yml for judge-service
spring:
  datasource:
    url: jdbc:mysql://${DB_HOST}:3306/${DB_NAME}?connectTimeout=10000&socketTimeout=30000
  rabbitmq:
    connection-timeout: 10000
    template:
      retry:
        enabled: true
        initial-interval: 1000
        max-attempts: 3
        multiplier: 2

server:
  port: 8084

judge:
  timeout:
    sql-execution:
      easy: 10000
      medium: 30000
      hard: 60000
    total-judgment:
      easy: 30000
      medium: 60000
      hard: 120000

container-manager:
  url: http://container-manager:8085
  acquire-timeout: 10000
  retry:
    max-attempts: 3
    initial-interval: 1000

ai-service:
  url: https://api.openai.com/v1
  timeout: 30000
  retry:
    max-attempts: 2
```

### 6.2 Docker Compose 环境变量

```yaml
# docker-compose.yml
services:
  judge-service:
    environment:
      - CONTAINER_MANAGER_URL=http://container-manager:8085
      - CONTAINER_ACQUIRE_TIMEOUT=10000
      - SQL_EXECUTION_TIMEOUT_EASY=10000
      - SQL_EXECUTION_TIMEOUT_MEDIUM=30000
      - SQL_EXECUTION_TIMEOUT_HARD=60000
      - TOTAL_JUDGMENT_TIMEOUT_EASY=30000
      - TOTAL_JUDGMENT_TIMEOUT_MEDIUM=60000
      - TOTAL_JUDGMENT_TIMEOUT_HARD=120000
      - AI_SERVICE_TIMEOUT=30000
      - RABBITMQ_RETRY_MAX_ATTEMPTS=3
```

---

## 8. 监控指标

| 指标 | 告警阈值 | 说明 |
|------|----------|------|
| HTTP 请求平均延迟 | > 5s | 接口响应过慢 |
| SQL 执行平均时间 | > 难度阈值的 80% | 可能需要优化 |
| 容器获取成功率 | < 95% | 容器池可能不足 |
| RabbitMQ 投递成功率 | < 99% | MQ 可能存在问题 |
| AI 服务调用成功率 | < 90% | AI 服务可能不稳定 |
| 判题任务超时率 | > 5% | 需要关注 |

---

## 9. 提交历史保留策略

### 8.1 历史记录范围

每个学生在每道题目上的提交历史记录策略：

| 保留项 | 数量 | 说明 |
|--------|------|------|
| 完整提交记录 | 最近 10 次 | 包含 SQL 内容、判题结果 |
| 判题结果快照 | 最近 10 次 | 存储判题详情 |
| 最终成绩记录 | 全部 | 只记录最高分/最新状态 |

### 8.2 提交历史数据结构

```sql
-- 完整提交记录（保留最近 10 次）
CREATE TABLE submission_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    submission_id BIGINT NOT NULL,
    problem_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    sql_content TEXT NOT NULL,
    status ENUM('PENDING', 'JUDGING', 'SUCCESS', 'FAILED') NOT NULL,
    submitted_at TIMESTAMP NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,  -- 标记是否为当前有效记录
    INDEX idx_student_problem (student_id, problem_id),
    INDEX idx_active (is_active),
    INDEX idx_submitted_at (submitted_at)
);

-- 判题结果快照（保留最近 10 次）
CREATE TABLE judge_result_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    result_id BIGINT NOT NULL,
    submission_id BIGINT NOT NULL,
    overall_score DECIMAL(5,2),
    overall_status VARCHAR(50),
    execution_time_ms BIGINT,
    created_at TIMESTAMP NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    INDEX idx_submission (submission_id),
    INDEX idx_active (is_active)
);
```

### 8.3 历史记录管理

```java
// 提交时判断是否需要归档旧记录
public void handleSubmission(Long studentId, Long problemId) {
    // 1. 查询该学生在该题目的历史提交数
    int historyCount = submissionHistoryRepository
        .countByStudentIdAndProblemId(studentId, problemId);

    if (historyCount >= MAX_HISTORY_SIZE) {
        // 2. 删除最早的记录
        submissionHistoryRepository.deleteOldest(studentId, problemId, 1);
    }

    // 3. 创建新记录
    submissionHistoryRepository.save(new SubmissionHistory(...));
}
```

### 8.4 归档策略

| 数据类型 | 保留策略 | 说明 |
|----------|----------|------|
| 最近 10 次完整提交 | 完整记录 | 包含 SQL 原文 |
| 10 次之前的记录 | 仅元数据 | 不保留 SQL 内容 |
| 判题结果快照 | 最近 10 次 | 详细判题信息 |
| 最终成绩 | 永久保留 | 用于统计和排行 |

### 8.5 配置项

```yaml
# 提交历史配置
submission:
  history:
    max-active-records: 10       # 活跃记录数（保留完整信息）
    archive-sql-after: 10        # 超过此数量后不保存 SQL 原文
    retention-days: 365         # 归档数据保留天数
```

---

## 10. 总结

| 配置项 | 推荐值 | 可调整范围 |
|--------|--------|------------|
| HTTP 请求超时 | 30s | 10-60s |
| MySQL 连接超时 | 10s | 5-30s |
| SQL 执行超时 | 10-60s | 按难度 |
| RabbitMQ 投递重试 | 3次 | 2-5次 |
| 容器获取重试 | 3次 | 2-5次 |
| AI 服务重试 | 2次 | 1-3次 |
| 判题总超时 | 30-120s | 按难度 |
