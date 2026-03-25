# SQL Judge Engine - 前端API文档

> **版本**: v1.0  
> **更新日期**: 2026-03-25  
> **适用对象**: 前端开发者

---

## 基础信息

### Base URL

```
http://localhost:8080
```

> 注意：前端只能通过 Gateway (Nginx) 访问API，端口为 **8080**

### 认证方式

- **类型**: JWT (JSON Web Token)
- **Header**: `Authorization: Bearer {token}`
- **Token有效期**: 24小时 (86400秒)

### 前端可访问的API前缀

| 前缀 | 对应服务 | 说明 |
|------|----------|------|
| `/api/user` | User Service | 用户相关接口 |
| `/api/problem` | Problem Service | 题目相关接口 |
| `/api/submission` | Submission Service | 提交相关接口 |
| `/api/result` | Result Service | 结果相关接口 |

### 前端不可访问的API (返回403)

- `/api/judge` - 判题服务（内部服务）
- `/api/container` - 容器管理服务（内部服务）
- `/api/internal` - 内部接口

---

## 接口概览

### 用户模块 (/api/user)

| 方法 | 路径 | 描述 | 权限 |
|------|------|------|------|
| POST | `/api/user/register` | 用户注册 | 公开 |
| POST | `/api/user/login` | 用户登录 | 公开 |
| GET | `/api/user/me` | 获取当前用户信息 | 需登录 |
| GET | `/api/user/{userId}` | 根据ID获取用户信息 | 需登录 |

### 题目模块 (/api/problem)

| 方法 | 路径 | 描述 | 权限 |
|------|------|------|------|
| GET | `/api/problem` | 分页查询题目列表 | 需登录 |
| GET | `/api/problem/{problemId}` | 获取题目详情 | 需登录 |
| GET | `/api/problem/teacher/my` | 获取当前教师的题目列表 | 教师 |
| POST | `/api/problem` | 创建题目 | 教师 |
| PUT | `/api/problem/{problemId}` | 更新题目 | 教师(仅自己创建) |
| DELETE | `/api/problem/{problemId}` | 删除题目 | 教师(仅自己创建) |
| PUT | `/api/problem/{problemId}/status` | 更新题目状态 | 教师(仅自己创建) |
| POST | `/api/problem/batch` | 批量导入题目 | 教师 |
| GET | `/api/tag` | 获取所有标签 | 需登录 |
| POST | `/api/tag` | 创建标签 | 需登录 |
| PUT | `/api/problem/{problemId}/tags` | 更新题目标签 | 需登录 |

### 提交模块 (/api/submission)

| 方法 | 路径 | 描述 | 权限 |
|------|------|------|------|
| POST | `/api/submission` | 提交SQL答案 | 学生 |
| GET | `/api/submission` | 分页查询提交历史 | 学生(仅自己) |
| GET | `/api/submission/{submissionId}` | 获取提交详情 | 需登录 |
| GET | `/api/submission/{submissionId}/status` | 查询判题状态 | 需登录 |

### 结果模块 (/api/result)

| 方法 | 路径 | 描述 | 权限 |
|------|------|------|------|
| GET | `/api/result/submission/{submissionId}` | 获取判题结果 | 需登录 |
| GET | `/api/result/student/{studentId}` | 获取学生成绩列表 | 学生(仅自己)/教师 |
| GET | `/api/result/problem/{problemId}/leaderboard` | 获取题目排行榜 | 需登录 |
| GET | `/api/result/leaderboard` | 获取总排行榜 | 需登录 |
| GET | `/api/result/statistics` | 获取判题统计 | 教师 |

---

## 用户模块 (/api/user)

### 1. 用户注册

注册新用户账号。

#### 请求

```http
POST /api/user/register
Content-Type: application/json
```

**请求体:**

| 参数名 | 类型 | 必填 | 说明 | 约束 |
|--------|------|------|------|------|
| username | string | 是 | 用户名 | 3-50字符 |
| password | string | 是 | 密码 | 6-100字符 |
| role | string | 是 | 用户角色 | `STUDENT` 或 `TEACHER` |
| email | string | 否 | 邮箱 | 有效的邮箱格式 |

**请求示例:**

```json
{
  "username": "student001",
  "password": "password123",
  "role": "STUDENT",
  "email": "student@example.com"
}
```

#### 响应

**成功响应 (201 Created):**

| 字段 | 类型 | 说明 |
|------|------|------|
| userId | integer | 用户ID |
| username | string | 用户名 |
| role | string | 用户角色 |

```json
{
  "userId": 1,
  "username": "student001",
  "role": "STUDENT"
}
```

**错误响应:**

| 状态码 | 说明 |
|--------|------|
| 400 | 参数校验失败 |
| 409 | 用户已存在 |

---

### 2. 用户登录

用户登录获取JWT Token。

#### 请求

```http
POST /api/user/login
Content-Type: application/json
```

**请求体:**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| username | string | 是 | 用户名 |
| password | string | 是 | 密码 |

**请求示例:**

```json
{
  "username": "student001",
  "password": "password123"
}
```

#### 响应

**成功响应 (200 OK):**

| 字段 | 类型 | 说明 |
|------|------|------|
| token | string | JWT Token |
| tokenType | string | Token类型，固定值 `Bearer` |
| expiresIn | integer | Token有效期（秒） |
| userId | integer | 用户ID |
| username | string | 用户名 |
| role | string | 用户角色 |

```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "userId": 1,
  "username": "student001",
  "role": "STUDENT"
}
```

**错误响应:**

| 状态码 | 说明 |
|--------|------|
| 401 | 用户名或密码错误 |
| 403 | 账号被禁用 |

---

### 3. 获取当前用户信息

获取当前登录用户的详细信息。

#### 请求

```http
GET /api/user/me
Authorization: Bearer {token}
```

#### 响应

**成功响应 (200 OK):**

| 字段 | 类型 | 说明 |
|------|------|------|
| userId | integer | 用户ID |
| username | string | 用户名 |
| role | string | 用户角色 |
| email | string | 邮箱 |
| createdAt | string | 创建时间（ISO 8601格式） |

```json
{
  "userId": 1,
  "username": "student001",
  "role": "STUDENT",
  "email": "student@example.com",
  "createdAt": "2024-01-15T10:00:00"
}
```

**错误响应:**

| 状态码 | 说明 |
|--------|------|
| 401 | Token缺失或过期 |

---

### 4. 根据ID获取用户信息

根据用户ID获取用户信息。

#### 请求

```http
GET /api/user/{userId}
Authorization: Bearer {token}
```

**路径参数:**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | integer | 是 | 用户ID |

#### 响应

**成功响应 (200 OK):**

响应格式与 `/api/user/me` 相同。

**错误响应:**

| 状态码 | 说明 |
|--------|------|
| 401 | Token缺失或过期 |
| 404 | 用户不存在 |

---

## 题目模块 (/api/problem)

### 1. 分页查询题目列表

获取分页的题目列表，支持筛选条件。

#### 请求

```http
GET /api/problem?page={page}&size={size}&difficulty={difficulty}&sqlType={sqlType}
Authorization: Bearer {token}
```

**查询参数:**

| 参数名 | 类型 | 必填 | 说明 | 默认值 |
|--------|------|------|------|--------|
| page | integer | 否 | 页码 | 1 |
| size | integer | 否 | 每页大小 | 10 |
| difficulty | string | 否 | 难度筛选 (`EASY`, `MEDIUM`, `HARD`) | - |
| sqlType | string | 否 | SQL类型筛选 (`DQL`, `DML`, `DDL`, `DCL`) | - |

#### 响应

**成功响应 (200 OK):**

| 字段 | 类型 | 说明 |
|------|------|------|
| content | array | 题目列表 |
| page | integer | 当前页码 |
| size | integer | 每页大小 |
| totalElements | integer | 总记录数 |
| totalPages | integer | 总页数 |

```json
{
  "content": [
    {
      "problemId": 1,
      "title": "查询年龄大于18岁的用户",
      "description": "从users表中查询...",
      "difficulty": "EASY",
      "sqlType": "DQL",
      "aiAssisted": false,
      "status": "PUBLISHED",
      "expectedType": "RESULT_SET",
      "teacherId": 1,
      "createdAt": "2024-01-15T10:00:00",
      "tags": [
        {
          "tagId": 1,
          "name": "SELECT",
          "color": "#3B82F6",
          "createdAt": "2024-01-15T09:00:00"
        }
      ]
    }
  ],
  "page": 1,
  "size": 10,
  "totalElements": 50,
  "totalPages": 5
}
```

---

### 2. 获取题目详情

获取指定题目的详细信息。

#### 请求

```http
GET /api/problem/{problemId}
Authorization: Bearer {token}
```

**路径参数:**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| problemId | integer | 是 | 题目ID |

#### 响应

**成功响应 (200 OK):**

根据调用者角色返回不同内容：

**教师查看 - 完整信息:**

```json
{
  "responseType": "detail",
  "problemId": 1,
  "title": "查询年龄大于18岁的用户",
  "description": "从users表中查询...",
  "difficulty": "EASY",
  "sqlType": "DQL",
  "aiAssisted": false,
  "status": "PUBLISHED",
  "expectedType": "RESULT_SET",
  "teacherId": 1,
  "createdAt": "2024-01-15T10:00:00",
  "initSql": "CREATE TABLE users (id INT, name VARCHAR(50), age INT); INSERT INTO users VALUES (1, '张三', 20), (2, '李四', 16);",
  "standardAnswer": "SELECT name, age FROM users WHERE age > 18",
  "expectedResult": "{\"columns\":[\"name\",\"age\"],\"rows\":[[\"张三\",20]]}",
  "tags": []
}
```

**学生查看 - 基础信息:**

```json
{
  "responseType": "basic",
  "problemId": 1,
  "title": "查询年龄大于18岁的用户",
  "description": "从users表中查询...",
  "difficulty": "EASY",
  "sqlType": "DQL",
  "expectedType": "RESULT_SET",
  "teacherId": 1,
  "createdAt": "2024-01-15T10:00:00"
}
```

**错误响应:**

| 状态码 | 说明 |
|--------|------|
| 401 | 未授权 |
| 404 | 题目不存在 |

---

### 3. 获取当前教师的题目列表

获取当前登录教师创建的所有题目。

#### 请求

```http
GET /api/problem/teacher/my?page={page}&size={size}&status={status}
Authorization: Bearer {token}
```

**查询参数:**

| 参数名 | 类型 | 必填 | 说明 | 默认值 |
|--------|------|------|------|--------|
| page | integer | 否 | 页码 | 1 |
| size | integer | 否 | 每页大小 | 10 |
| status | string | 否 | 状态筛选 (`DRAFT`, `READY`, `PUBLISHED`, `ARCHIVED`) | - |

#### 响应

**成功响应 (200 OK):**

响应格式与「分页查询题目列表」相同。

**错误响应:**

| 状态码 | 说明 |
|--------|------|
| 403 | 仅教师可访问 |

---

### 4. 创建题目

创建新题目（仅教师）。

#### 请求

```http
POST /api/problem
Authorization: Bearer {token}
Content-Type: application/json
```

**请求体:**

| 参数名 | 类型 | 必填 | 说明 | 约束 |
|--------|------|------|------|------|
| title | string | 是 | 题目标题 | 最多200字符 |
| description | string | 否 | 题目描述 | - |
| difficulty | string | 否 | 难度 | `EASY`, `MEDIUM`(默认), `HARD` |
| sqlType | string | 是 | SQL类型 | `DQL`, `DML`, `DDL`, `DCL` |
| aiAssisted | boolean | 否 | 启用AI辅助判题 | 默认`false`（DDL/DCL有效） |
| initSql | string | 否 | 初始化SQL脚本 | 建表、插入数据等 |
| standardAnswer | string | 否 | 标准答案SQL | 系统自动生成期望结果 |
| expectedType | string | 否 | 期望结果类型 | `RESULT_SET`, `DATA_SNAPSHOT`, `METADATA`, `PRIVILEGE` |

**请求示例:**

```json
{
  "title": "查询年龄大于18岁的用户",
  "description": "从users表中查询所有年龄大于18岁的用户，返回用户名和年龄",
  "difficulty": "EASY",
  "sqlType": "DQL",
  "initSql": "CREATE TABLE users (id INT, name VARCHAR(50), age INT); INSERT INTO users VALUES (1, '张三', 20), (2, '李四', 16);",
  "standardAnswer": "SELECT name, age FROM users WHERE age > 18"
}
```

#### 响应

**成功响应 (201 Created):**

| 字段 | 类型 | 说明 |
|------|------|------|
| problemId | integer | 题目ID |
| title | string | 题目标题 |
| description | string | 题目描述 |
| difficulty | string | 难度 |
| sqlType | string | SQL类型 |
| aiAssisted | boolean | 是否启用AI辅助 |
| status | string | 题目状态 |
| expectedType | string | 期望结果类型 |
| teacherId | integer | 创建教师ID |
| createdAt | string | 创建时间 |
| tags | array | 标签列表 |

```json
{
  "problemId": 1,
  "title": "查询年龄大于18岁的用户",
  "description": "从users表中查询所有年龄大于18岁的用户...",
  "difficulty": "EASY",
  "sqlType": "DQL",
  "aiAssisted": false,
  "status": "DRAFT",
  "expectedType": "RESULT_SET",
  "teacherId": 1,
  "createdAt": "2024-01-15T10:00:00",
  "tags": []
}
```

**错误响应:**

| 状态码 | 说明 |
|--------|------|
| 400 | 参数校验失败 |
| 401 | 未授权 |
| 403 | 仅教师可创建题目 |

---

### 5. 更新题目

更新指定题目的信息（仅教师，仅自己创建的题目）。

#### 请求

```http
PUT /api/problem/{problemId}
Authorization: Bearer {token}
Content-Type: application/json
```

**路径参数:**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| problemId | integer | 是 | 题目ID |

**请求体:**

所有字段均为可选，只更新提供的字段：

| 参数名 | 类型 | 说明 |
|--------|------|------|
| title | string | 题目标题 |
| description | string | 题目描述 |
| difficulty | string | 难度 (`EASY`, `MEDIUM`, `HARD`) |
| aiAssisted | boolean | 是否启用AI辅助 |
| initSql | string | 初始化SQL |
| standardAnswer | string | 标准答案 |
| expectedType | string | 期望结果类型 |

**请求示例:**

```json
{
  "title": "查询年龄大于18岁的用户（更新）",
  "difficulty": "MEDIUM"
}
```

#### 响应

**成功响应 (200 OK):**

返回更新后的题目信息，格式与创建题目响应相同。

**错误响应:**

| 状态码 | 说明 |
|--------|------|
| 403 | 仅题目创建者可更新 |
| 404 | 题目不存在 |

---

### 6. 删除题目

删除指定题目（仅教师，仅自己创建的题目）。

#### 请求

```http
DELETE /api/problem/{problemId}
Authorization: Bearer {token}
```

**路径参数:**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| problemId | integer | 是 | 题目ID |

#### 响应

**成功响应 (204 No Content):**

无响应体。

**错误响应:**

| 状态码 | 说明 |
|--------|------|
| 403 | 仅题目创建者可删除 |
| 404 | 题目不存在 |

---

### 7. 更新题目状态

更新题目的发布状态（仅教师，仅自己创建的题目）。

#### 状态流转

```
DRAFT -> READY -> PUBLISHED -> ARCHIVED
```

| 状态 | 说明 |
|------|------|
| DRAFT | 草稿状态，可编辑 |
| READY | 准备就绪，待发布 |
| PUBLISHED | 已发布，学生可见 |
| ARCHIVED | 已归档，不再使用 |

#### 请求

```http
PUT /api/problem/{problemId}/status
Authorization: Bearer {token}
Content-Type: application/json
```

**路径参数:**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| problemId | integer | 是 | 题目ID |

**请求体:**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| status | string | 是 | 新状态 |

**请求示例:**

```json
{
  "status": "PUBLISHED"
}
```

#### 响应

**成功响应 (200 OK):**

返回更新后的题目信息。

**错误响应:**

| 状态码 | 说明 |
|--------|------|
| 400 | 无效的状态流转 |
| 403 | 仅教师可操作 |

---

### 8. 批量导入题目

批量导入多个题目（仅教师）。

#### 请求

```http
POST /api/problem/batch
Authorization: Bearer {token}
Content-Type: application/json
```

**请求体:**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| problems | array | 是 | 题目列表 |

**请求示例:**

```json
{
  "problems": [
    {
      "title": "题目1",
      "description": "描述1",
      "difficulty": "EASY",
      "sqlType": "DQL",
      "initSql": "CREATE TABLE t1 (id INT);",
      "standardAnswer": "SELECT * FROM t1"
    },
    {
      "title": "题目2",
      "description": "描述2",
      "difficulty": "MEDIUM",
      "sqlType": "DML"
    }
  ]
}
```

#### 响应

**成功响应 (201 Created):**

| 字段 | 类型 | 说明 |
|------|------|------|
| successCount | integer | 成功导入数量 |
| failCount | integer | 失败数量 |
| problems | array | 成功导入的题目列表 |
| errors | array | 错误信息列表 |

```json
{
  "successCount": 1,
  "failCount": 1,
  "problems": [
    {
      "problemId": 2,
      "title": "题目1"
    }
  ],
  "errors": [
    {
      "index": 1,
      "title": "题目2",
      "error": "SQL type is required"
    }
  ]
}
```

---

### 9. 获取所有标签

获取系统中所有的标签列表。

#### 请求

```http
GET /api/tag
Authorization: Bearer {token}
```

#### 响应

**成功响应 (200 OK):**

返回标签列表。

```json
[
  {
    "tagId": 1,
    "name": "SELECT",
    "color": "#3B82F6",
    "createdAt": "2024-01-15T10:00:00"
  },
  {
    "tagId": 2,
    "name": "JOIN",
    "color": "#10B981",
    "createdAt": "2024-01-15T10:00:00"
  }
]
```

---

### 10. 创建标签

创建新标签。

#### 请求

```http
POST /api/tag
Authorization: Bearer {token}
Content-Type: application/json
```

**请求体:**

| 参数名 | 类型 | 必填 | 说明 | 默认值 |
|--------|------|------|------|--------|
| name | string | 是 | 标签名称 | - |
| color | string | 否 | 标签颜色 | `#3B82F6` |

**请求示例:**

```json
{
  "name": "AGGREGATE",
  "color": "#F59E0B"
}
```

#### 响应

**成功响应 (201 Created):**

```json
{
  "tagId": 3,
  "name": "AGGREGATE",
  "color": "#F59E0B",
  "createdAt": "2024-01-15T10:00:00"
}
```

---

### 11. 更新题目标签

更新指定题目的标签关联。

#### 请求

```http
PUT /api/problem/{problemId}/tags
Authorization: Bearer {token}
Content-Type: application/json
```

**路径参数:**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| problemId | integer | 是 | 题目ID |

**请求体:**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| tagIds | array | 是 | 标签ID列表 |

**请求示例:**

```json
{
  "tagIds": [1, 2, 3]
}
```

#### 响应

**成功响应 (200 OK):**

```json
{
  "tags": [
    {
      "tagId": 1,
      "name": "SELECT",
      "color": "#3B82F6",
      "createdAt": "2024-01-15T10:00:00"
    },
    {
      "tagId": 2,
      "name": "JOIN",
      "color": "#10B981",
      "createdAt": "2024-01-15T10:00:00"
    }
  ]
}
```

---

## 提交模块 (/api/submission)

### 1. 提交SQL答案

学生提交SQL答案进行判题。

#### 请求

```http
POST /api/submission
Authorization: Bearer {token}
Content-Type: application/json
```

**请求体:**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| problemId | integer | 是 | 题目ID |
| sqlContent | string | 是 | SQL答案内容 |

**请求示例:**

```json
{
  "problemId": 1,
  "sqlContent": "SELECT name, age FROM users WHERE age > 18"
}
```

#### 响应

**成功响应 (202 Accepted):**

提交成功，已进入判题队列。

| 字段 | 类型 | 说明 |
|------|------|------|
| submissionId | integer | 提交ID |
| problemId | integer | 题目ID |
| problemTitle | string | 题目标题 |
| status | string | 提交状态 |
| submittedAt | string | 提交时间 |

```json
{
  "submissionId": 100,
  "problemId": 1,
  "problemTitle": "查询年龄大于18岁的用户",
  "status": "PENDING",
  "submittedAt": "2024-01-15T10:30:00"
}
```

**状态说明:**

| 状态 | 说明 |
|------|------|
| PENDING | 待处理，已入队 |
| JUDGING | 正在判题 |
| SUCCESS | 判题完成 |
| FAILED | 判题失败 |

**错误响应:**

| 状态码 | 说明 |
|--------|------|
| 400 | 请求参数错误（如SQL为空） |
| 401 | 未授权 |
| 403 | 仅学生可提交 |
| 404 | 题目不存在 |
| 503 | 判题服务不可用 |

---

### 2. 分页查询提交历史

查询当前学生的提交历史记录。

#### 请求

```http
GET /api/submission?page={page}&size={size}&problemId={problemId}&status={status}&sort={sort}
Authorization: Bearer {token}
```

**查询参数:**

| 参数名 | 类型 | 必填 | 说明 | 默认值 |
|--------|------|------|------|--------|
| page | integer | 否 | 页码 | 1 |
| size | integer | 否 | 每页大小 | 10 |
| problemId | integer | 否 | 按题目ID筛选 | - |
| status | string | 否 | 按状态筛选 | - |
| sort | string | 否 | 排序字段 | `submittedAt,desc` |

**排序选项:**

- `submittedAt,desc` - 提交时间倒序（默认）
- `submittedAt,asc` - 提交时间正序
- `score,desc` - 分数倒序

#### 响应

**成功响应 (200 OK):**

| 字段 | 类型 | 说明 |
|------|------|------|
| content | array | 提交记录列表 |
| page | integer | 当前页码 |
| size | integer | 每页大小 |
| totalElements | integer | 总记录数 |
| totalPages | integer | 总页数 |

```json
{
  "content": [
    {
      "submissionId": 100,
      "problemId": 1,
      "problemTitle": "查询年龄大于18岁的用户",
      "status": "SUCCESS",
      "submittedAt": "2024-01-15T10:30:00"
    },
    {
      "submissionId": 99,
      "problemId": 1,
      "problemTitle": "查询年龄大于18岁的用户",
      "status": "SUCCESS",
      "submittedAt": "2024-01-15T10:25:00"
    }
  ],
  "page": 1,
  "size": 10,
  "totalElements": 25,
  "totalPages": 3
}
```

---

### 3. 获取提交详情

获取指定提交的详细信息。

#### 请求

```http
GET /api/submission/{submissionId}
Authorization: Bearer {token}
```

**路径参数:**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| submissionId | integer | 是 | 提交ID |

#### 响应

**成功响应 (200 OK):**

| 字段 | 类型 | 说明 |
|------|------|------|
| submissionId | integer | 提交ID |
| problemId | integer | 题目ID |
| problemTitle | string | 题目标题 |
| sqlContent | string | 提交的SQL内容 |
| status | string | 提交状态 |
| score | number | 得分(0-100) |
| executionTimeMs | integer | 执行时间(毫秒) |
| judgeStatus | string | 判题状态 |
| errorMessage | string | 错误信息 |
| submittedAt | string | 提交时间 |

**判题状态说明:**

| 状态 | 说明 |
|------|------|
| CORRECT | 答案正确 |
| INCORRECT | 答案错误 |
| TIME_LIMIT | 执行超时 |
| ERROR | 执行出错 |
| AI_APPROVED | AI审核通过（DDL/DCL） |
| AI_REJECTED | AI审核拒绝（DDL/DCL） |

```json
{
  "submissionId": 100,
  "problemId": 1,
  "problemTitle": "查询年龄大于18岁的用户",
  "sqlContent": "SELECT name, age FROM users WHERE age > 18",
  "status": "SUCCESS",
  "score": 100.00,
  "executionTimeMs": 45,
  "judgeStatus": "CORRECT",
  "errorMessage": null,
  "submittedAt": "2024-01-15T10:30:00"
}
```

**错误响应:**

| 状态码 | 说明 |
|--------|------|
| 401 | 未授权 |
| 404 | 提交不存在 |

---

### 4. 查询判题状态

快速查询提交的判题状态（用于轮询）。

#### 请求

```http
GET /api/submission/{submissionId}/status
Authorization: Bearer {token}
```

**路径参数:**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| submissionId | integer | 是 | 提交ID |

#### 响应

**成功响应 (200 OK):**

| 字段 | 类型 | 说明 |
|------|------|------|
| submissionId | integer | 提交ID |
| status | string | 提交状态 |
| score | number | 得分(0-100，判题完成后有值) |
| judgeStatus | string | 判题状态(判题完成后有值) |

**判题完成示例:**

```json
{
  "submissionId": 100,
  "status": "SUCCESS",
  "score": 100.00,
  "judgeStatus": "CORRECT"
}
```

**判题中示例:**

```json
{
  "submissionId": 101,
  "status": "JUDGING",
  "score": null,
  "judgeStatus": null
}
```

**错误响应:**

| 状态码 | 说明 |
|--------|------|
| 401 | 未授权 |
| 404 | 提交不存在 |

---

## 结果模块 (/api/result)

### 1. 获取判题结果

获取指定提交的判题结果详情。

#### 请求

```http
GET /api/result/submission/{submissionId}
Authorization: Bearer {token}
```

**路径参数:**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| submissionId | integer | 是 | 提交ID |

#### 响应

**成功响应 (200 OK):**

| 字段 | 类型 | 说明 |
|------|------|------|
| resultId | integer | 结果ID |
| submissionId | integer | 提交ID |
| problemId | integer | 题目ID |
| problemTitle | string | 题目标题 |
| studentId | integer | 学生ID |
| studentUsername | string | 学生用户名 |
| score | number | 得分(0-100) |
| status | string | 判题状态 |
| executionTimeMs | integer | 执行时间(毫秒) |
| errorMessage | string | 错误信息 |
| createdAt | string | 结果创建时间 |

**判题状态说明:**

| 状态 | 说明 |
|------|------|
| CORRECT | 答案正确 |
| INCORRECT | 答案错误 |
| TIME_LIMIT | 执行超时 |
| ERROR | 执行出错 |
| AI_APPROVED | AI审核通过（DDL/DCL） |
| AI_REJECTED | AI审核拒绝（DDL/DCL） |

```json
{
  "resultId": 50,
  "submissionId": 100,
  "problemId": 1,
  "problemTitle": "查询年龄大于18岁的用户",
  "studentId": 1,
  "studentUsername": "student001",
  "score": 100.00,
  "status": "CORRECT",
  "executionTimeMs": 45,
  "errorMessage": null,
  "createdAt": "2024-01-15T10:30:05"
}
```

**错误响应:**

| 状态码 | 说明 |
|--------|------|
| 401 | 未授权 |
| 404 | 结果不存在 |

---

### 2. 获取学生成绩列表

获取指定学生的成绩列表。

#### 请求

```http
GET /api/result/student/{studentId}?page={page}&size={size}
Authorization: Bearer {token}
```

**路径参数:**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| studentId | integer | 是 | 学生ID |

**查询参数:**

| 参数名 | 类型 | 必填 | 说明 | 默认值 |
|--------|------|------|------|--------|
| page | integer | 否 | 页码 | 1 |
| size | integer | 否 | 每页大小 | 10 |

#### 响应

**成功响应 (200 OK):**

| 字段 | 类型 | 说明 |
|------|------|------|
| content | array | 成绩列表 |
| page | integer | 当前页码 |
| size | integer | 每页大小 |
| totalElements | integer | 总记录数 |
| totalPages | integer | 总页数 |

```json
{
  "content": [
    {
      "resultId": 50,
      "submissionId": 100,
      "createdAt": "2024-01-15T10:30:05"
    },
    {
      "resultId": 45,
      "submissionId": 95,
      "createdAt": "2024-01-15T10:15:00"
    }
  ],
  "page": 1,
  "size": 10,
  "totalElements": 15,
  "totalPages": 2
}
```

**错误响应:**

| 状态码 | 说明 |
|--------|------|
| 401 | 未授权 |
| 403 | 学生无权查看他人成绩 |

---

### 3. 获取题目排行榜

获取指定题目的排行榜。

#### 请求

```http
GET /api/result/problem/{problemId}/leaderboard?page={page}&size={size}
Authorization: Bearer {token}
```

**路径参数:**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| problemId | integer | 是 | 题目ID |

**查询参数:**

| 参数名 | 类型 | 必填 | 说明 | 默认值 |
|--------|------|------|------|--------|
| page | integer | 否 | 页码 | 1 |
| size | integer | 否 | 每页大小 | 50 |

#### 响应

**成功响应 (200 OK):**

| 字段 | 类型 | 说明 |
|------|------|------|
| problemId | integer | 题目ID |
| problemTitle | string | 题目标题 |
| entries | array | 排行榜条目 |
| page | integer | 当前页码 |
| size | integer | 每页大小 |
| totalElements | integer | 总记录数 |
| totalPages | integer | 总页数 |

**排行榜条目:**

| 字段 | 类型 | 说明 |
|------|------|------|
| rank | integer | 排名 |
| studentId | integer | 学生ID |
| studentUsername | string | 学生用户名 |
| bestScore | number | 最高分 |
| latestSubmitTime | string | 最新提交时间 |

```json
{
  "problemId": 1,
  "problemTitle": "查询年龄大于18岁的用户",
  "entries": [
    {
      "rank": 1,
      "studentId": 1,
      "studentUsername": "student001",
      "bestScore": 100.00,
      "latestSubmitTime": "2024-01-15T10:30:00"
    },
    {
      "rank": 2,
      "studentId": 2,
      "studentUsername": "student002",
      "bestScore": 95.00,
      "latestSubmitTime": "2024-01-15T11:00:00"
    }
  ],
  "page": 1,
  "size": 10,
  "totalElements": 50,
  "totalPages": 5
}
```

---

### 4. 获取总排行榜

获取所有学生的总排行榜。

#### 请求

```http
GET /api/result/leaderboard?page={page}&size={size}
Authorization: Bearer {token}
```

**查询参数:**

| 参数名 | 类型 | 必填 | 说明 | 默认值 |
|--------|------|------|------|--------|
| page | integer | 否 | 页码 | 1 |
| size | integer | 否 | 每页大小 | 50 |

#### 响应

**成功响应 (200 OK):**

| 字段 | 类型 | 说明 |
|------|------|------|
| entries | array | 排行榜条目 |
| page | integer | 当前页码 |
| size | integer | 每页大小 |
| totalElements | integer | 总记录数 |
| totalPages | integer | 总页数 |

**排行榜条目:**

| 字段 | 类型 | 说明 |
|------|------|------|
| rank | integer | 排名 |
| studentId | integer | 学生ID |
| studentUsername | string | 学生用户名 |
| totalScore | number | 总得分 |
| problemsSolved | integer | 解题数量 |
| totalSubmissions | integer | 总提交次数 |

```json
{
  "entries": [
    {
      "rank": 1,
      "studentId": 1,
      "studentUsername": "student001",
      "totalScore": 850.00,
      "problemsSolved": 10,
      "totalSubmissions": 15
    },
    {
      "rank": 2,
      "studentId": 2,
      "studentUsername": "student002",
      "totalScore": 780.00,
      "problemsSolved": 9,
      "totalSubmissions": 20
    }
  ],
  "page": 1,
  "size": 20,
  "totalElements": 100,
  "totalPages": 5
}
```

---

### 5. 获取判题统计

获取判题统计信息（仅教师）。

#### 请求

```http
GET /api/result/statistics?problemId={problemId}
Authorization: Bearer {token}
```

**查询参数:**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| problemId | integer | 否 | 按题目筛选（不提供则统计全部） |

#### 响应

**成功响应 (200 OK):**

| 字段 | 类型 | 说明 |
|------|------|------|
| problemId | integer | 题目ID（如按题目筛选） |
| totalSubmissions | integer | 总提交数 |
| acceptedSubmissions | integer | 正确提交数 |
| acceptanceRate | number | 通过率(0-100) |
| averageScore | number | 平均分 |
| maxScore | number | 最高分 |
| minScore | number | 最低分 |
| averageExecutionTime | integer | 平均执行时间(毫秒) |

```json
{
  "problemId": 1,
  "totalSubmissions": 100,
  "acceptedSubmissions": 75,
  "acceptanceRate": 75.00,
  "averageScore": 82.50,
  "maxScore": 100.00,
  "minScore": 0.00,
  "averageExecutionTime": 45
}
```

**错误响应:**

| 状态码 | 说明 |
|--------|------|
| 401 | 未授权 |
| 403 | 仅教师可访问 |

---

## 错误处理

### 错误响应格式

所有API错误响应遵循统一格式：

```json
{
  "code": 401,
  "message": "未授权或Token已过期",
  "timestamp": "2024-01-15T10:30:00"
}
```

### 常见错误码

| 状态码 | 说明 | 处理方式 |
|--------|------|----------|
| 400 | 请求参数错误 | 检查请求参数是否符合要求 |
| 401 | 未授权 | Token缺失或过期，需要重新登录 |
| 403 | 禁止访问 | 用户权限不足，无法执行此操作 |
| 404 | 资源不存在 | 请求的资源不存在 |
| 409 | 资源冲突 | 如用户名已存在 |
| 429 | 请求过于频繁 | 触发限流，稍后再试 |
| 500 | 服务器内部错误 | 服务端错误，联系管理员 |
| 503 | 服务不可用 | 判题服务暂时不可用 |

### 401 未授权处理

当Token过期或无效时：

1. 清除本地存储的Token
2. 跳转到登录页面
3. 提示用户重新登录

```javascript
// 响应拦截器示例
axios.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);
```

---

## 权限控制

### 角色说明

| 角色 | 值 | 说明 |
|------|-----|------|
| 学生 | `STUDENT` | 可以查看题目、提交答案、查看自己的成绩 |
| 教师 | `TEACHER` | 可以创建题目、管理题目、查看所有学生成绩 |

### 角色权限矩阵

| 功能 | 学生 | 教师 |
|------|------|------|
| 用户注册/登录 | ✓ | ✓ |
| 查看题目列表 | ✓ | ✓ |
| 查看题目详情 | ✓ (基础信息) | ✓ (完整信息) |
| 创建题目 | ✗ | ✓ |
| 更新题目 | ✗ | ✓ (仅自己创建) |
| 删除题目 | ✗ | ✓ (仅自己创建) |
| 提交SQL答案 | ✓ | ✗ |
| 查看提交历史 | ✓ (仅自己) | ✗ |
| 查看判题结果 | ✓ | ✓ |
| 查看排行榜 | ✓ | ✓ |
| 查看统计信息 | ✗ | ✓ |

---

## 前端实现示例

### 1. Axios 配置

```typescript
// api/client.ts
import axios from 'axios';

const apiClient = axios.create({
  baseURL: 'http://localhost:8080',
  headers: {
    'Content-Type': 'application/json'
  }
});

// 请求拦截器 - 自动添加Token
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// 响应拦截器 - 处理401错误
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('userId');
      localStorage.removeItem('role');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default apiClient;
```

### 2. API 封装

```typescript
// api/index.ts
import apiClient from './client';

// 用户API
export const userApi = {
  register: (data: RegisterRequest) => 
    apiClient.post('/api/user/register', data),
  login: (data: LoginRequest) => 
    apiClient.post('/api/user/login', data),
  getCurrentUser: () => 
    apiClient.get('/api/user/me'),
  getUserById: (userId: number) => 
    apiClient.get(`/api/user/${userId}`)
};

// 题目API
export const problemApi = {
  getProblems: (params?: ProblemQueryParams) => 
    apiClient.get('/api/problem', { params }),
  getProblem: (problemId: number) => 
    apiClient.get(`/api/problem/${problemId}`),
  getMyProblems: (params?: ProblemQueryParams) => 
    apiClient.get('/api/problem/teacher/my', { params }),
  createProblem: (data: CreateProblemRequest) => 
    apiClient.post('/api/problem', data),
  updateProblem: (problemId: number, data: UpdateProblemRequest) => 
    apiClient.put(`/api/problem/${problemId}`, data),
  deleteProblem: (problemId: number) => 
    apiClient.delete(`/api/problem/${problemId}`),
  updateProblemStatus: (problemId: number, status: string) => 
    apiClient.put(`/api/problem/${problemId}/status`, { status }),
  getTags: () => 
    apiClient.get('/api/tag'),
  createTag: (data: CreateTagRequest) => 
    apiClient.post('/api/tag', data),
  updateProblemTags: (problemId: number, tagIds: number[]) => 
    apiClient.put(`/api/problem/${problemId}/tags`, { tagIds })
};

// 提交API
export const submissionApi = {
  submit: (data: SubmitRequest) => 
    apiClient.post('/api/submission', data),
  getSubmissions: (params?: SubmissionQueryParams) => 
    apiClient.get('/api/submission', { params }),
  getSubmission: (submissionId: number) => 
    apiClient.get(`/api/submission/${submissionId}`),
  getSubmissionStatus: (submissionId: number) => 
    apiClient.get(`/api/submission/${submissionId}/status`)
};

// 结果API
export const resultApi = {
  getResult: (submissionId: number) => 
    apiClient.get(`/api/result/submission/${submissionId}`),
  getStudentResults: (studentId: number, params?: PaginationParams) => 
    apiClient.get(`/api/result/student/${studentId}`, { params }),
  getProblemLeaderboard: (problemId: number, params?: PaginationParams) => 
    apiClient.get(`/api/result/problem/${problemId}/leaderboard`, { params }),
  getOverallLeaderboard: (params?: PaginationParams) => 
    apiClient.get('/api/result/leaderboard', { params }),
  getStatistics: (problemId?: number) => 
    apiClient.get('/api/result/statistics', { 
      params: problemId ? { problemId } : undefined 
    })
};
```

### 3. 类型定义

```typescript
// types/index.ts

// 用户相关
export interface UserInfo {
  userId: number;
  username: string;
  role: 'STUDENT' | 'TEACHER';
  email?: string;
  createdAt: string;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  tokenType: string;
  expiresIn: number;
  userId: number;
  username: string;
  role: string;
}

export interface RegisterRequest {
  username: string;
  password: string;
  role: 'STUDENT' | 'TEACHER';
  email?: string;
}

// 题目相关
export interface Problem {
  problemId: number;
  title: string;
  description?: string;
  difficulty: 'EASY' | 'MEDIUM' | 'HARD';
  sqlType: 'DQL' | 'DML' | 'DDL' | 'DCL';
  aiAssisted: boolean;
  status: 'DRAFT' | 'READY' | 'PUBLISHED' | 'ARCHIVED';
  expectedType: 'RESULT_SET' | 'DATA_SNAPSHOT' | 'METADATA' | 'PRIVILEGE';
  teacherId: number;
  createdAt: string;
  tags: Tag[];
  // 教师查看时包含
  initSql?: string;
  standardAnswer?: string;
  expectedResult?: string;
}

export interface Tag {
  tagId: number;
  name: string;
  color: string;
  createdAt: string;
}

export interface CreateProblemRequest {
  title: string;
  description?: string;
  difficulty?: 'EASY' | 'MEDIUM' | 'HARD';
  sqlType: 'DQL' | 'DML' | 'DDL' | 'DCL';
  aiAssisted?: boolean;
  initSql?: string;
  standardAnswer?: string;
  expectedType?: 'RESULT_SET' | 'DATA_SNAPSHOT' | 'METADATA' | 'PRIVILEGE';
}

export interface ProblemQueryParams {
  page?: number;
  size?: number;
  difficulty?: string;
  sqlType?: string;
  status?: string;
}

// 提交相关
export interface Submission {
  submissionId: number;
  problemId: number;
  problemTitle: string;
  status: 'PENDING' | 'JUDGING' | 'SUCCESS' | 'FAILED';
  submittedAt: string;
}

export interface SubmissionDetail extends Submission {
  sqlContent: string;
  score?: number;
  executionTimeMs?: number;
  judgeStatus?: 'CORRECT' | 'INCORRECT' | 'TIME_LIMIT' | 'ERROR' | 'AI_APPROVED' | 'AI_REJECTED';
  errorMessage?: string;
}

export interface SubmitRequest {
  problemId: number;
  sqlContent: string;
}

export interface SubmissionQueryParams {
  page?: number;
  size?: number;
  problemId?: number;
  status?: string;
  sort?: string;
}

// 结果相关
export interface JudgeResult {
  resultId: number;
  submissionId: number;
  problemId: number;
  problemTitle: string;
  studentId: number;
  studentUsername: string;
  score: number;
  status: 'CORRECT' | 'INCORRECT' | 'TIME_LIMIT' | 'ERROR' | 'AI_APPROVED' | 'AI_REJECTED';
  executionTimeMs?: number;
  errorMessage?: string;
  createdAt: string;
}

export interface LeaderboardEntry {
  rank: number;
  studentId: number;
  studentUsername: string;
  bestScore: number;
  latestSubmitTime: string;
}

export interface OverallLeaderboardEntry {
  rank: number;
  studentId: number;
  studentUsername: string;
  totalScore: number;
  problemsSolved: number;
  totalSubmissions: number;
}

export interface Statistics {
  problemId?: number;
  totalSubmissions: number;
  acceptedSubmissions: number;
  acceptanceRate: number;
  averageScore: number;
  maxScore: number;
  minScore: number;
  averageExecutionTime: number;
}

// 分页响应
export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

// 错误响应
export interface ApiError {
  code: number;
  message: string;
  timestamp: string;
}
```

---

## 附录

### 状态码速查表

#### 题目状态

| 状态 | 说明 |
|------|------|
| DRAFT | 草稿状态，可编辑 |
| READY | 准备就绪，待发布 |
| PUBLISHED | 已发布，学生可见 |
| ARCHIVED | 已归档，不再使用 |

#### 提交状态

| 状态 | 说明 |
|------|------|
| PENDING | 待处理，已入队 |
| JUDGING | 正在判题 |
| SUCCESS | 判题完成 |
| FAILED | 判题失败 |

#### 判题结果状态

| 状态 | 说明 |
|------|------|
| CORRECT | 答案正确 |
| INCORRECT | 答案错误 |
| TIME_LIMIT | 执行超时 |
| ERROR | 执行出错 |
| AI_APPROVED | AI审核通过（DDL/DCL） |
| AI_REJECTED | AI审核拒绝（DDL/DCL） |

#### SQL类型

| 类型 | 说明 |
|------|------|
| DQL | 数据查询语言 (SELECT) |
| DML | 数据操作语言 (INSERT, UPDATE, DELETE) |
| DDL | 数据定义语言 (CREATE, ALTER, DROP) |
| DCL | 数据控制语言 (GRANT, REVOKE) |

#### 难度级别

| 级别 | 说明 |
|------|------|
| EASY | 简单 |
| MEDIUM | 中等 |
| HARD | 困难 |
