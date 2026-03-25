# SQL 判题引擎 - 黑盒测试文档

## 1. 测试理念

### 1.1 什么是黑盒测试

黑盒测试（Black Box Testing）是一种软件测试方法，在微服务架构下特指：

- **测试对象**：每个微服务作为独立黑盒
- **测试方式**：仅通过服务暴露的 API 接口进行测试
- **测试原则**：不依赖服务内部实现，不 Mock 内部调用，不进行跨服务直接调用
- **验证目标**：输入请求 → 验证响应/副作用

### 1.2 微服务架构下的黑盒测试 vs 集成测试

| 维度 | 集成测试（原文档） | 黑盒测试（本文档） |
|------|-------------------|------------------|
| 测试视角 | 服务间协作视角 | 单服务独立视角 |
| 服务依赖 | 需要所有依赖服务运行 | 仅测试目标服务及其直接依赖 |
| Mock 使用 | Mock 服务间调用 | 不 Mock API 契约，仅 Mock 基础设施 |
| 测试粒度 | 跨服务流程 | 单服务功能 |
| 失败定位 | 依赖服务间契约 | 单服务边界 |

### 1.3 本项目黑盒测试原则

根据 [microservices.md](file:///home/xingyu/projects/mysql-db-practice/sql-judge-engine/docs/architecture/microservices.md) 中的服务拆分：

```
公网服务 (通过 gateway 访问):
  - user-service:8081  ✅ 黑盒测试
  - problem-service:8082  ✅ 黑盒测试
  - submission-service:8083  ✅ 黑盒测试
  - result-service:8086 (查询接口)  ✅ 黑盒测试

内部服务 (不暴露公网):
  - judge-service:8084  ❌ 不进行黑盒测试（无 HTTP API）
  - container-manager:8085  ❌ 不进行黑盒测试（仅内部调用）
  - result-service:8086 (回写接口)  ❌ 不进行黑盒测试（仅内部调用）
```

---

## 2. 服务 API 边界定义

### 2.1 user-service (端口 8081)

**黑盒边界**：仅测试通过 Gateway 暴露的 API

| 端点 | 方法 | 认证 | 黑盒测试方式 |
|------|------|------|-------------|
| `/api/user/register` | POST | 无 | ✅ 发送请求，验证响应 |
| `/api/user/login` | POST | 无 | ✅ 发送请求，验证 JWT |
| `/api/user/me` | GET | JWT | ✅ 先登录获取 JWT，再测试 |
| `/api/user/{userId}` | GET | JWT | ✅ 先登录获取 JWT，再测试 |

**数据结构**（依据 integration-test-architecture.md）：

```json
RegisterRequest: { "username": "string", "password": "string", "role": "TEACHER|STUDENT", "email": "string" }
LoginRequest: { "username": "string", "password": "string" }
LoginResponse: { "token": "jwt-string", "expiresIn": 86400 }
UserInfo: { "id": 1, "username": "string", "role": "TEACHER|STUDENT", "email": "string" }
```

### 2.2 problem-service (端口 8082)

**黑盒边界**：通过 Gateway 暴露的 API

| 端点 | 方法 | 认证 | 黑盒测试方式 |
|------|------|------|-------------|
| `/api/problem` | POST | X-User-Id | ✅ 发送请求，验证响应 |
| `/api/problem` | GET | 无 | ✅ 分页列表测试 |
| `/api/problem/teacher/my` | GET | X-User-Id | ✅ 教师题目列表 |
| `/api/problem/{problemId}` | GET | 无 | ✅ 题目详情 |
| `/api/problem/{problemId}` | PUT | X-User-Id | ✅ 更新题目 |
| `/api/problem/{problemId}` | DELETE | X-User-Id | ✅ 删除题目 |
| `/api/problem/{problemId}/status` | PUT | X-User-Id | ✅ 状态变更 |
| `/api/problem/batch` | POST | X-User-Id | ✅ 批量导入 |

**数据结构**（依据 integration-test-architecture.md）：

```json
CreateProblemRequest: { "title": "string", "description": "string", "difficulty": "EASY|MEDIUM|HARD",
                        "sqlType": "DQL|DML|DDL|DCL", "initSql": "string", "standardAnswer": "string" }
ProblemResponse: { "id": 1, "title": "string", "status": "DRAFT|READY|PUBLISHED|ARCHIVED", ... }
```

### 2.3 submission-service (端口 8083)

**黑盒边界**：通过 Gateway 暴露的 API

| 端点 | 方法 | 认证 | 黑盒测试方式 |
|------|------|------|-------------|
| `/api/submission` | POST | X-User-Id | ✅ 提交答案 |
| `/api/submission` | GET | X-User-Id | ✅ 提交列表 |
| `/api/submission/{submissionId}` | GET | 无 | ✅ 提交详情 |
| `/api/submission/{submissionId}/status` | GET | 无 | ✅ 判题状态 |

**数据结构**（依据 integration-test-architecture.md）：

```json
CreateSubmissionRequest: { "problemId": 100, "sqlContent": "SELECT * FROM users" }
SubmissionResponse: { "submissionId": 123, "status": "PENDING|JUDGING|SUCCESS|FAILED" }
SubmissionDetailResponse: { "submissionId": 123, "problemId": 100, "sqlContent": "string",
                           "status": "string", "submittedAt": "timestamp" }
```

**注意**：提交后通过 RabbitMQ 异步判题，结果存储在 result-service

### 2.4 result-service (端口 8086)

**黑盒边界**：通过 Gateway 暴露的查询接口

| 端点 | 方法 | 认证 | 黑盒测试方式 |
|------|------|------|-------------|
| `/api/result/submission/{submissionId}` | GET | JWT | ✅ 查询某提交结果 |
| `/api/result/student/{studentId}` | GET | 无 | ✅ 学生结果列表 |
| `/api/result/problem/{problemId}/leaderboard` | GET | 无 | ✅ 题目排行榜 |
| `/api/result/leaderboard` | GET | 无 | ✅ 总排行榜 |

**数据结构**（依据 integration-test-architecture.md）：

```json
JudgeResult: { "submissionId": 123, "score": 100.00, "status": "CORRECT|INCORRECT|TIME_LIMIT|ERROR",
               "errorMessage": null, "executionTimeMs": 150 }
```

### 2.5 不进行黑盒测试的服务

以下服务根据 [microservices.md](file:///home/xingyu/projects/mysql-db-practice/sql-judge-engine/docs/architecture/microservices.md) 不进行黑盒测试：

| 服务 | 原因 | 测试方式 |
|------|------|----------|
| **judge-service** | 无 HTTP API，仅消费 RabbitMQ | 端到端测试（通过 submission-service 触发） |
| **container-manager** | 仅供内部服务调用，不暴露公网 | 端到端测试（通过 judge-service 间接验证） |
| **result-service POST 接口** | 仅供 judge-service 回写 | 端到端测试 |

---

## 3. 黑盒测试用例

### 3.1 user-service 黑盒测试

#### TC-USER-001: 用户注册成功

**前置条件**：数据库无重名用户

**测试步骤**：
```
POST /api/user/register
Body: {
    "username": "testteacher",
    "password": "Password123!",
    "role": "TEACHER",
    "email": "teacher@test.com"
}
```

**预期结果**：
- HTTP 201 Created
- 响应体包含 `id`, `username`, `role`

**黑盒验证点**：仅验证 API 响应，不验证数据库内部状态

---

#### TC-USER-002: 用户注册失败 - 用户名已存在

**前置条件**：用户名 "existinguser" 已存在

**测试步骤**：
```
POST /api/user/register
Body: {
    "username": "existinguser",
    "password": "Password123!",
    "role": "STUDENT",
    "email": "student@test.com"
}
```

**预期结果**：
- HTTP 409 Conflict

---

#### TC-USER-003: 用户登录成功

**前置条件**：用户已注册

**测试步骤**：
```
POST /api/user/login
Body: {
    "username": "testteacher",
    "password": "Password123!"
}
```

**预期结果**：
- HTTP 200 OK
- 响应体包含 `token` (JWT 格式)
- `token` 可用于后续认证请求

---

#### TC-USER-004: 用户登录失败 - 密码错误

**前置条件**：用户已注册

**测试步骤**：
```
POST /api/user/login
Body: {
    "username": "testteacher",
    "password": "WrongPassword!"
}
```

**预期结果**：
- HTTP 401 Unauthorized

---

#### TC-USER-005: 获取当前用户信息（需认证）

**前置条件**：已登录获取有效 JWT

**测试步骤**：
```
GET /api/user/me
Header: Authorization: Bearer <jwt_token>
```

**预期结果**：
- HTTP 200 OK
- 响应体包含用户信息

---

### 3.2 problem-service 黑盒测试

#### TC-PROB-001: 教师创建题目

**前置条件**：教师用户已登录，JWT 可用

**测试步骤**：
```
POST /api/problem
Header: X-User-Id: <teacher_id>
Body: {
    "title": "简单查询",
    "description": "查询所有用户",
    "difficulty": "EASY",
    "sqlType": "DQL",
    "initSql": "CREATE TABLE users (id INT, name VARCHAR(50))",
    "standardAnswer": "SELECT * FROM users"
}
```

**预期结果**：
- HTTP 201 Created
- 响应体包含 `id`, `title`, `status: DRAFT`

---

#### TC-PROB-002: 学生查询题目列表（无需认证）

**前置条件**：题目已发布

**测试步骤**：
```
GET /api/problem?page=0&size=10&difficulty=EASY&sqlType=DQL
```

**预期结果**：
- HTTP 200 OK
- 分页响应，包含题目列表

---

#### TC-PROB-003: 教师更新题目状态

**前置条件**：教师拥有题目，题目状态为 DRAFT

**测试步骤**：
```
PUT /api/problem/{problemId}/status
Header: X-User-Id: <teacher_id>
Body: {
    "status": "PUBLISHED"
}
```

**预期结果**：
- HTTP 200 OK
- 题目状态变更为 PUBLISHED

---

#### TC-PROB-004: 教师删除题目

**前置条件**：教师拥有题目

**测试步骤**：
```
DELETE /api/problem/{problemId}
Header: X-User-Id: <teacher_id>
```

**预期结果**：
- HTTP 204 No Content

---

### 3.3 submission-service 黑盒测试

#### TC-SUB-001: 学生提交 SQL 答案

**前置条件**：
- 学生用户已登录
- 题目已发布 (problemId 已知)

**测试步骤**：
```
POST /api/submission
Header: X-User-Id: <student_id>
Body: {
    "problemId": 100,
    "sqlContent": "SELECT * FROM users"
}
```

**预期结果**：
- HTTP 202 Accepted
- 响应体包含 `submissionId`, `status: PENDING`

**黑盒验证点**：仅验证提交接口响应，判题结果异步存储到 result-service

---

#### TC-SUB-002: 学生查询提交状态

**前置条件**：已提交过答案

**测试步骤**：
```
GET /api/submission/{submissionId}/status
```

**预期结果**：
- HTTP 200 OK
- 响应体包含 `status: PENDING|JUDGING|SUCCESS|FAILED`

---

#### TC-SUB-003: 学生查询提交历史

**前置条件**：学生有提交记录

**测试步骤**：
```
GET /api/submission?page=0&size=10
Header: X-User-Id: <student_id>
```

**预期结果**：
- HTTP 200 OK
- 分页响应，包含提交列表

---

### 3.4 result-service 黑盒测试

#### TC-RESULT-001: 查询某提交的判题结果

**前置条件**：提交已完成判题

**测试步骤**：
```
GET /api/result/submission/{submissionId}
Header: Authorization: Bearer <jwt_token>
```

**预期结果**：
- HTTP 200 OK
- 响应体包含 `score`, `status: CORRECT|INCORRECT|ERROR`, `executionTimeMs`

**验证依据**：根据 [data-flow.md](file:///home/xingyu/projects/mysql-db-practice/sql-judge-engine/docs/architecture/data-flow.md) 2.5 节，判题完成后结果存储到此

---

#### TC-RESULT-002: 查询学生成绩列表

**前置条件**：学生有判题结果

**测试步骤**：
```
GET /api/result/student/{studentId}
```

**预期结果**：
- HTTP 200 OK
- 响应体包含成绩列表

---

#### TC-RESULT-003: 查询题目排行榜

**前置条件**：有学生完成题目

**测试步骤**：
```
GET /api/result/problem/{problemId}/leaderboard
```

**预期结果**：
- HTTP 200 OK
- 响应体包含排行榜数据

---

## 4. 端到端黑盒测试（跨服务）

端到端测试通过完整的用户操作流程验证系统可用性，不 Mock 任何服务。

### 4.1 完整答题流程 E2E

#### TC-E2E-001: 教师创建题目 → 学生答题 → 查看成绩

**前置条件**：
- 教师账户 (teacher_id=1)
- 学生账户 (student_id=2)

**测试步骤**：

```
Step 1: 教师登录
POST /api/user/login
Body: { "username": "teacher1", "password": "password" }
→ 获取 teacher_token

Step 2: 教师创建题目
POST /api/problem
Header: X-User-Id: 1
Body: {
    "title": "E2E测试题目",
    "description": "查询所有用户",
    "difficulty": "EASY",
    "sqlType": "DQL",
    "initSql": "CREATE TABLE users (id INT, name VARCHAR(50)); INSERT INTO users VALUES (1, 'Alice')",
    "standardAnswer": "SELECT * FROM users"
}
→ 获取 problem_id

Step 3: 教师发布题目
PUT /api/problem/{problem_id}/status
Header: X-User-Id: 1
Body: { "status": "PUBLISHED" }

Step 4: 学生登录
POST /api/user/login
Body: { "username": "student1", "password": "password" }
→ 获取 student_token

Step 5: 学生提交答案
POST /api/submission
Header: X-User-Id: 2
Body: {
    "problemId": {problem_id},
    "sqlContent": "SELECT * FROM users"
}
→ 获取 submission_id

Step 6: 轮询等待判题完成 (最多 60 秒)
GET /api/submission/{submission_id}/status
→ 验证 status = SUCCESS 或 FAILED

Step 7: 学生查询成绩
GET /api/result/submission/{submission_id}
Header: Authorization: Bearer <student_token>
→ 验证响应包含 score, status
```

**预期结果**：
- 所有 API 调用返回正确状态码
- 最终能查询到判题结果

**失败定位**：
- Step 1-4 失败 → user-service 或 problem-service 问题
- Step 5 失败 → submission-service 问题
- Step 6-7 失败 → judge-service / result-service 问题

---

### 4.2 错误处理 E2E

#### TC-E2E-002: 提交到不存在的题目

**测试步骤**：
```
POST /api/submission
Header: X-User-Id: 2
Body: {
    "problemId": 99999,
    "sqlContent": "SELECT 1"
}

GET /api/submission/{submission_id}/status
→ 验证状态变化
```

**预期结果**：
- 提交接口返回 202 Accepted（异步处理）
- 最终状态应为 ERROR 或 FAILED

---

## 5. 黑盒测试环境要求

### 5.1 最小测试环境

根据 [microservices.md](file:///home/xingyu/projects/mysql-db-practice/sql-judge-engine/docs/architecture/microservices.md) 7.1 节：

```
┌─────────────────────────────────────────────────────┐
│                   Docker Network                     │
│                                                      │
│  ┌─────────┐  ┌─────────┐  ┌─────────┐             │
│  │ Gateway │  │  User   │  │Problem  │             │
│  │ :8080   │  │ :8081   │  │ :8082   │             │
│  └────┬────┘  └─────────┘  └─────────┘             │
│       │                                                │
│  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐ │
│  │Submit   │  │ Result* │  │Container*│           │
│  │ :8083   │  │ :8086   │  │ :8085   │           │
│  └─────────┘  └─────────┘  └─────────┘           │
│                    ↑                                 │
│         (内部服务，通过判题流程间接验证)              │
│                                                      │
│  ┌─────────────────────────────────────────────────┐ │
│  │              MySQL (业务库) :3306               │ │
│  └─────────────────────────────────────────────────┘ │
│                                                      │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐               │
│  │Container│ │Container│ │Container│  (判题容器池) │
│  │  MySQL  │ │  MySQL  │ │  MySQL  │               │
│  └─────────┘ └─────────┘ └─────────┘               │
│                                                      │
│  ┌─────────────────────────────────────────────────┐ │
│  │              RabbitMQ :5672                      │ │
│  └─────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────┘
```

### 5.2 黑盒测试基础设施 Mock

| 组件 | Mock 方案 | 说明 |
|------|----------|------|
| MySQL (业务库) | Testcontainers | 每个服务独立 MySQL |
| RabbitMQ | Testcontainers | 消息队列测试 |
| Docker (容器池) | **不 Mock** | 通过完整判题流程验证 |

**重要原则**：黑盒测试不 Mock 服务间 API 契约调用

---

## 6. 测试数据准备

### 6.1 最小测试数据集

```
Users:
- id=1, username="teacher1", role=TEACHER, password=BCrypt("password")
- id=2, username="student1", role=STUDENT, password=BCrypt("password")
- id=3, username="student2", role=STUDENT, password=BCrypt("password")

Problems:
- id=100, title="简单查询", sqlType=DQL, status=PUBLISHED, teacherId=1
- id=101, title="插入数据", sqlType=DML, status=PUBLISHED, teacherId=1
- id=102, title="创建表", sqlType=DDL, status=PUBLISHED, aiAssisted=true, teacherId=1
```

### 6.2 测试数据清理

每个测试用例执行后应清理：
- 创建的用户
- 创建的题目
- 创建的提交记录
- 产生的判题结果

---

## 7. 黑盒测试执行

### 7.1 执行顺序

```
1. user-service 测试 (TC-USER-*)
   ↓
2. problem-service 测试 (TC-PROB-*)
   ↓
3. submission-service 测试 (TC-SUB-*)
   ↓
4. result-service 测试 (TC-RESULT-*)
   ↓
5. 端到端测试 (TC-E2E-*)
```

### 7.2 失败处理

| 测试失败 | 可能原因 | 定位方式 |
|----------|----------|----------|
| TC-USER-* | user-service | 查看 user-service 日志 |
| TC-PROB-* | problem-service | 查看 problem-service 日志 |
| TC-SUB-* | submission-service | 查看 submission-service + RabbitMQ |
| TC-RESULT-* | result-service | 查看 result-service 日志 |
| TC-E2E-* | 需逐级定位 | 按执行顺序日志排查 |

---

## 8. 与原集成测试文档对比

| 原集成测试问题 | 本黑盒测试改进 |
|--------------|---------------|
| 假设可以直接调用内部服务 | 明确区分公网服务和内部服务 |
| Mock 服务间调用 | 仅 Mock 基础设施，不 Mock API 契约 |
| 测试跨服务流程 | 先测单服务，再做端到端 |
| 未区分 HTTP API 测试和消息队列测试 | 明确哪些通过 API 测试，哪些通过 E2E 测试 |
| 测试 container-manager | 黑盒无法测试，仅通过判题流程间接验证 |

---

## 9. 附录：测试覆盖矩阵

| 服务 | API 测试 | 端到端测试 | 黑盒可行 |
|------|----------|-----------|----------|
| user-service | ✅ TC-USER-* | ✅ E2E-001 | ✅ |
| problem-service | ✅ TC-PROB-* | ✅ E2E-001 | ✅ |
| submission-service | ✅ TC-SUB-* | ✅ E2E-001 | ✅ |
| result-service (查询) | ✅ TC-RESULT-* | ✅ E2E-001 | ✅ |
| result-service (回写) | ❌ | ✅ E2E-001 | ❌ (内部接口) |
| judge-service | ❌ | ✅ E2E-001 | ❌ (无 HTTP API) |
| container-manager | ❌ | ✅ E2E-001 | ❌ (内部接口) |
