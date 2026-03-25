# SQL Judge Engine 集成测试架构文档

## 1. 服务依赖图

```
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                              SQL Judge Engine 架构                                   │
├─────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                     │
│   ┌──────────────┐                                                                  │
│   │  user-service │  (8081)                                                          │
│   │  JWT认证      │                                                                  │
│   └──────┬───────┘                                                                  │
│          │                                                                          │
│          │ X-User-Id Header                                                         │
│          ▼                                                                          │
│   ┌──────────────┐     ┌──────────────┐     ┌──────────────┐                       │
│   │problem-service│     │submission-   │     │ result-service│  (8086)              │
│   │   (8082)      │     │  service     │     │  X-API-Key   │                       │
│   └──────┬───────┘     │   (8083)      │     └──────┬───────┘                       │
│          │              └──────┬───────┘              │                               │
│          │                     │                      │                               │
│          │                     │ X-User-Id            │ HTTP POST                     │
│          │                     ▼                      │ /api/result                  │
│          │              ┌──────────────┐              │                               │
│          │              │  RabbitMQ     │              │                               │
│          │              │ judge-tasks  │              │                               │
│          │              └──────┬───────┘              │                               │
│          │                     │                      │                               │
│          │ HTTP GET            │                      │                               │
│          │ /api/problem/{id}   │                      │                               │
│          ▼                     ▼                      │                               │
│   ┌──────────────────────────────────────┐            │                               │
│   │           judge-service (8084)        │◄───────────┘                               │
│   │  ┌─────────────────────────────────┐  │                                          │
│   │  │  JudgeTaskConsumer              │  │  RabbitMQ Consumer                       │
│   │  │  1. Get ProblemDetail           │──┼──► problem-service                      │
│   │  │  2. Acquire Container          │──┼──► container-manager                     │
│   │  │  3. Execute SQL                │  │     (Docker MySQL)                        │
│   │  │  4. Submit Result              │──┼──► result-service                         │
│   │  └─────────────────────────────────┘  │                                          │
│   └──────────────────────────────────────┘                                          │
│                                                                                     │
│   ┌──────────────────────────────────────┐                                          │
│   │        container-manager (8085)        │                                          │
│   │  Docker容器池管理                       │                                          │
│   │  MySQL容器获取/释放                     │                                          │
│   └──────────────────────────────────────┘                                          │
│                                                                                     │
└─────────────────────────────────────────────────────────────────────────────────────┘
```

## 2. 服务详细分析

### 2.1 user-service (端口 8081)

#### API端点与DTO

| 端点 | 方法 | 认证 | 请求/响应 |
|------|------|------|----------|
| `/api/user/register` | POST | 无 | RegisterRequest -> RegisterResponse |
| `/api/user/login` | POST | 无 | LoginRequest -> LoginResponse (含JWT) |
| `/api/user/me` | GET | JWT | -> UserInfo |
| `/api/user/{userId}` | GET | JWT | -> UserInfo |

#### 数据库实体

```
users表:
- id (PK)
- username (unique)
- password (bcrypt)
- role (TEACHER/STUDENT)
- email
- created_at
- updated_at
```

#### 认证机制

- **JWT**: 使用`jwt.secret`配置的密钥签名，24小时过期
- **密码加密**: BCrypt
- **安全配置**: 除`/register`, `/login`, `/swagger-ui/**`, `/actuator/**`外均需认证

---

### 2.2 problem-service (端口 8082)

#### API端点与DTO

| 端点 | 方法 | 认证 | 请求/响应 |
|------|------|------|----------|
| `/api/problem` | POST | X-User-Id | CreateProblemRequest -> ProblemResponse |
| `/api/problem` | GET | 无 | 分页列表 (difficulty, sqlType过滤) |
| `/api/problem/teacher/my` | GET | X-User-Id | 教师的题目列表 |
| `/api/problem/{problemId}` | GET | 无 | -> ProblemBasicResponse |
| `/api/problem/{problemId}` | PUT | X-User-Id | UpdateProblemRequest -> ProblemResponse |
| `/api/problem/{problemId}` | DELETE | X-User-Id | -> 204 |
| `/api/problem/{problemId}/status` | PUT | X-User-Id | UpdateProblemStatusRequest -> ProblemResponse |
| `/api/problem/batch` | POST | X-User-Id | BatchImportRequest -> BatchImportResponse |

#### 数据库实体

```
problems表:
- id (PK)
- title, description, difficulty, sql_type, status
- ai_assisted, init_sql, standard_answer, expected_result, expected_type
- teacher_id, created_at, updated_at

tags表: id, name
problem_tags表: problem_id, tag_id
```

---

### 2.3 submission-service (端口 8083)

#### API端点与DTO

| 端点 | 方法 | 认证 | 请求/响应 |
|------|------|------|----------|
| `/api/submission` | POST | X-User-Id | CreateSubmissionRequest -> SubmissionResponse (202) |
| `/api/submission` | GET | X-User-Id | 分页列表 (problemId, status过滤) |
| `/api/submission/{submissionId}` | GET | 无 | -> SubmissionDetailResponse |
| `/api/submission/{submissionId}/status` | GET | 无 | -> SubmissionStatusResponse |

#### RabbitMQ配置

```yaml
Exchange: judge-exchange (Direct)
Queue: judge-tasks
Routing Key: judge.task
Dead Letter Exchange: judge-dlx
Dead Letter Queue: judge-tasks-dlq
```

#### 消息格式 (JudgeTaskMessage)

```json
{
  "messageId": "uuid-string",
  "submissionId": 123,
  "problemId": 456,
  "sqlContent": "SELECT * FROM users",
  "studentId": 789,
  "timeLimit": 30,
  "maxMemory": 1024,
  "retryCount": 0,
  "timestamp": "2024-01-15T10:30:00"
}
```

---

### 2.4 judge-service (端口 8084)

#### 核心组件

1. **JudgeTaskConsumer**: RabbitMQ消费者，监听judge-tasks队列
2. **JudgeService**: 评判逻辑编排
3. **ContainerClientService**: 调用container-manager获取MySQL容器
4. **ProblemClientService**: 调用problem-service获取题目详情
5. **ResultClientService**: 调用result-service提交结果
6. **SqlExecutorService**: 在MySQL容器中执行SQL
7. **AiJudgeService**: AI辅助判题服务（DDL/DCL类型）

#### 依赖服务

| 服务 | URL | 认证 | 用途 |
|------|-----|------|------|
| problem-service | http://localhost:8082 | 无 | 获取题目详情 |
| container-manager | http://localhost:8085 | 无 | 获取/释放MySQL容器 |
| result-service | http://localhost:8086 | X-API-Key | 提交评判结果 |
| AI服务 | https://dashscope.aliyuncs.com | API Key | DDL/DCL语义分析 |

---

### 2.5 result-service (端口 8086)

#### API端点与DTO

| 端点 | 方法 | 认证 | 用途 |
|------|------|------|------|
| `/api/result` | POST | X-API-Key | 创建评判结果 (内部) |
| `/api/result/submission/{submissionId}` | GET | JWT | 获取某提交的结果 |
| `/api/result/student/{studentId}` | GET | 无 | 学生结果列表 |
| `/api/result/problem/{problemId}/leaderboard` | GET | 无 | 题目排行榜 |
| `/api/result/leaderboard` | GET | 无 | 总排行榜 |

#### 数据库实体

```
judge_results表:
- id (PK)
- submission_id, problem_id, student_id
- score, status, error_message, execution_time_ms
- created_at
```

---

### 2.6 container-manager (端口 8085)

#### API端点与DTO

| 端点 | 方法 | 认证 | 请求/响应 |
|------|------|------|----------|
| `/api/container/acquire` | POST | 无 | AcquireContainerRequest -> ContainerInfoResponse |
| `/api/container/release` | POST | 无 | ReleaseContainerRequest -> 200 |
| `/api/container/{containerId}` | GET | 无 | -> ContainerInfoResponse |
| `/api/container/{containerId}` | DELETE | 无 | -> 204 |
| `/api/pool/status` | GET | 无 | -> PoolStatusResponse |

---

## 3. 服务间契约规格

### 3.1 JudgeTaskMessage (submission -> judge)

```json
{
  "messageId": "uuid-string",
  "submissionId": 123,
  "problemId": 456,
  "sqlContent": "SELECT * FROM users",
  "studentId": 789,
  "timeLimit": 30,
  "maxMemory": 1024,
  "retryCount": 0,
  "timestamp": "2024-01-15T10:30:00"
}
```

### 3.2 ProblemDetail (problem-service -> judge-service)

```json
{
  "problemId": 456,
  "title": "简单查询",
  "description": "查询所有用户",
  "difficulty": "EASY",
  "sqlType": "DQL",
  "aiAssisted": false,
  "status": "PUBLISHED",
  "expectedType": "RESULT_SET",
  "teacherId": 1,
  "initSql": "CREATE TABLE users (id INT, name VARCHAR(50))",
  "standardAnswer": null,
  "expectedResult": "[{\"id\":1,\"name\":\"Alice\"}]"
}
```

### 3.3 ContainerInfo (container-manager -> judge-service)

```json
{
  "containerId": "abc123def456",
  "containerName": "judge-container-123",
  "ipAddress": "172.17.0.2",
  "mysqlPort": 3307,
  "mysqlUser": "judge",
  "connectionToken": "temp-token-uuid",
  "tokenExpiresAt": "2024-01-15T10:35:00",
  "status": "IN_USE"
}
```

### 3.4 JudgeResult (judge-service -> result-service)

```json
{
  "submissionId": 123,
  "score": 100.00,
  "status": "CORRECT",
  "errorMessage": null,
  "executionTimeMs": 150
}
```

**状态说明**:
- `CORRECT`: 规则比对正确（DQL/DML）
- `INCORRECT`: 规则比对错误（DQL/DML）
- `AI_APPROVED`: AI判定正确（DDL/DCL，语义等价）
- `AI_REJECTED`: AI判定错误（DDL/DCL，语义不等价）
- `TIME_LIMIT`: 执行超时
- `ERROR`: 系统错误

---

## 4. 集成测试场景

### 4.1 核心评判流程测试

#### 场景: 完整的DQL评判流程

**前置条件**:
- user-service有注册用户 (teacherId=1, studentId=2)
- problem-service有已发布的DQL题目 (problemId=100)
- container-manager有可用容器
- result-service可访问

**测试步骤**:

```
1. 教师创建题目 (problem-service)
   POST /api/problem
   X-User-Id: 1
   Body: { "title": "查询所有用户", "sqlType": "DQL", ... }
   验证: 201 Created, problemId returned

2. 学生提交答案 (submission-service)
   POST /api/submission
   X-User-Id: 2
   Body: { "problemId": 100, "sqlContent": "SELECT * FROM users" }
   验证: 202 Accepted, submissionId returned

3. 验证RabbitMQ消息发送
   验证: judge-exchange上有消息

4. 验证评判完成 (judge-service消费消息)
   - 获取题目详情
   - 获取容器
   - 执行initSql和studentSql
   - 比较结果
   - 提交结果到result-service

5. 验证结果查询 (result-service)
   GET /api/result/submission/{submissionId}
   验证: status="CORRECT", score=100

6. 验证提交状态更新 (submission-service)
   GET /api/submission/{submissionId}/status
   验证: status="SUCCESS"
```

---

### 4.2 容器池管理测试

#### 场景: 容器获取和释放

**测试步骤**:

```
1. 获取池状态 (初始)
   GET /api/pool/status
   验证: availableContainers >= 1

2. 获取容器
   POST /api/container/acquire
   Body: { "problemId": 100, "timeout": 30000 }
   验证: ContainerInfoResponse with containerId, ipAddress, mysqlPort

3. 验证池状态更新
   GET /api/pool/status
   验证: availableContainers减少

4. 释放容器 (重置数据库)
   POST /api/container/release
   Body: { "containerId": "abc123", "resetDatabase": true }
   验证: 200 OK

5. 验证池状态恢复
   GET /api/pool/status
   验证: availableContainers恢复
```

---

### 4.3 错误处理测试

#### 场景: 题目不存在

```
1. 提交答案到不存在的题目
   POST /api/submission
   X-User-Id: 2
   Body: { "problemId": 99999, "sqlContent": "SELECT 1" }
   验证: 202 Accepted

2. judge-service处理时获取题目失败
   验证: 重试3次后失败

3. 验证错误结果
   GET /api/result/submission/{submissionId}
   验证: status="ERROR", errorMessage包含"Failed to fetch problem"
```

#### 场景: SQL执行超时

```
1. 创建耗时SQL的题目 (timeLimit=1秒)
2. 提交: SELECT SLEEP(10)
3. 验证: status="ERROR", errorMessage包含timeout
```

---

### 4.4 并发测试

#### 场景: 多学生同时提交

```
1. 准备10个学生账号
2. 同时提交10个评判请求
3. 验证:
   - 所有请求返回202
   - RabbitMQ有10条消息
   - judge-service依次处理
   - result-service有10条结果
```

---

## 5. 测试 Mock/测试夹具需求

### 5.1 基础设施 Mock

| 组件 | Mock方案 | 用途 |
|------|----------|------|
| MySQL | Testcontainers | 各服务的持久化 |
| RabbitMQ | Testcontainers (rabbitmq) | 消息队列测试 |
| Docker | Testcontainers 或 MockDockerClient | container-manager |

### 5.2 服务间调用 Mock

| 调用 | Mock方案 |
|------|----------|
| judge -> problem-service | RestTemplate mock / WireMock |
| judge -> container-manager | RestTemplate mock / WireMock |
| judge -> result-service | RestTemplate mock / WireMock |

### 5.3 建议的测试基类

```java
@SpringBootTest
@Testcontainers
@ActiveProfiles("integration-test")
public abstract class BaseIntegrationTest {
    // 通用测试配置和方法
}
```

---

## 6. 关键集成点清单

| 序号 | 集成点 | 风险等级 | 测试优先级 |
|------|--------|----------|-----------|
| 1 | submission -> RabbitMQ -> judge | 高 | P0 |
| 2 | judge -> problem-service 获取题目 | 高 | P0 |
| 3 | judge -> container-manager 容器管理 | 高 | P0 |
| 4 | judge -> result-service 结果提交 | 高 | P0 |
| 5 | judge 执行 SQL 并比较结果 | 高 | P0 |
| 6 | 容器池自动清理 | 中 | P1 |
| 7 | DDL/DCL AI评判 | 中 | P1 |
| 8 | 错误处理和重试机制 | 中 | P1 |
| 9 | 并发提交处理 | 中 | P1 |
| 10 | 容器复用和数据库重置 | 低 | P2 |

---

## 7. 测试数据准备

### 最小测试数据集

```
Users:
- id=1, username="teacher1", role=TEACHER
- id=2, username="student1", role=STUDENT
- id=3, username="student2", role=STUDENT

Problems:
- id=100, title="简单查询", sqlType=DQL, status=PUBLISHED, teacherId=1
- id=101, title="插入数据", sqlType=DML, status=PUBLISHED, teacherId=1
- id=102, title="创建表", sqlType=DDL, aiAssisted=true, teacherId=1

Containers:
- id=1, status=AVAILABLE
- id=2, status=AVAILABLE
```

---

## 8. 已知架构问题 (供参考)

1. **X-User-Id Header传递**: problem-service和submission-service使用简单header传递用户ID，存在伪造风险。

2. **无submission状态更新回调**: submission-service在提交后更新状态为JUDGING，但最终结果由judge-service直接写入result-service，没有回调更新submission状态。

3. **容器token安全**: connectionToken在传输中使用，应考虑加密。

4. **无服务间API版本控制**: 各服务HTTP API无版本控制。

5. **RabbitMQ消息无幂等性**: JudgeTaskMessage使用UUID作为messageId，但消费者未做幂等处理。
