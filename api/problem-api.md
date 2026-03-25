# 题目服务 API

> 基础URL: `http://localhost:8082`

## 接口概览

### 题目管理

| 方法 | 路径 | 描述 | 权限 |
|------|------|------|------|
| POST | `/api/problem` | 创建题目 | 教师 |
| GET | `/api/problem` | 分页查询题目列表 | 需登录 |
| GET | `/api/problem/teacher/my` | 获取当前教师的题目列表 | 教师 |
| GET | `/api/problem/{problemId}` | 获取题目详情 | 需登录 |
| PUT | `/api/problem/{problemId}` | 更新题目 | 教师(仅自己创建) |
| DELETE | `/api/problem/{problemId}` | 删除题目 | 教师(仅自己创建) |
| PUT | `/api/problem/{problemId}/status` | 更新题目状态 | 教师(仅自己创建) |
| POST | `/api/problem/batch` | 批量导入题目 | 教师 |

### 标签管理

| 方法 | 路径 | 描述 | 权限 |
|------|------|------|------|
| GET | `/api/tag` | 获取所有标签 | 需登录 |
| POST | `/api/tag` | 创建标签 | 需登录 |
| PUT | `/api/problem/{problemId}/tags` | 更新题目标签 | 需登录 |

---

## 题目管理

### 1. 创建题目

#### 基本信息

- **接口名称**: 创建题目
- **请求方法**: POST
- **请求URL**: `/api/problem`
- **权限要求**: 教师

#### 请求参数

**请求头:**

| 参数名 | 必填 | 说明 |
|--------|------|------|
| Content-Type | 是 | 固定值 `application/json` |
| Authorization | 是 | `Bearer {token}` |
| X-User-Id | 是 | 教师ID（网关自动添加） |

**请求体 (Body):**

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

#### 请求示例

```bash
curl -X POST http://localhost:8082/api/problem \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 1" \
  -d '{
    "title": "查询年龄大于18岁的用户",
    "description": "从users表中查询所有年龄大于18岁的用户，返回用户名和年龄",
    "difficulty": "EASY",
    "sqlType": "DQL",
    "initSql": "CREATE TABLE users (id INT, name VARCHAR(50), age INT); INSERT INTO users VALUES (1, '张三', 20), (2, '李四', 16);",
    "standardAnswer": "SELECT name, age FROM users WHERE age > 18"
  }'
```

#### 响应说明

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

**响应示例:**

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

### 2. 分页查询题目列表

#### 基本信息

- **接口名称**: 分页查询题目列表
- **请求方法**: GET
- **请求URL**: `/api/problem`
- **权限要求**: 需登录

#### 请求参数

**查询参数 (Query):**

| 参数名 | 类型 | 必填 | 说明 | 默认值 |
|--------|------|------|------|--------|
| page | integer | 否 | 页码 | 1 |
| size | integer | 否 | 每页大小 | 10 |
| difficulty | string | 否 | 难度筛选 | - |
| sqlType | string | 否 | SQL类型筛选 | - |

**请求头:**

| 参数名 | 必填 | 说明 |
|--------|------|------|
| Authorization | 是 | `Bearer {token}` |

#### 请求示例

```bash
# 基础查询
curl -X GET "http://localhost:8082/api/problem?page=1&size=10" \
  -H "Authorization: Bearer {token}"

# 带筛选条件
curl -X GET "http://localhost:8082/api/problem?page=1&size=10&difficulty=EASY&sqlType=DQL" \
  -H "Authorization: Bearer {token}"
```

#### 响应说明

**成功响应 (200 OK):**

| 字段 | 类型 | 说明 |
|------|------|------|
| content | array | 题目列表 |
| page | integer | 当前页码 |
| size | integer | 每页大小 |
| totalElements | integer | 总记录数 |
| totalPages | integer | 总页数 |

**响应示例:**

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

### 3. 获取当前教师的题目列表

#### 基本信息

- **接口名称**: 获取当前教师的题目列表
- **请求方法**: GET
- **请求URL**: `/api/problem/teacher/my`
- **权限要求**: 教师

#### 请求参数

**查询参数 (Query):**

| 参数名 | 类型 | 必填 | 说明 | 默认值 |
|--------|------|------|------|--------|
| page | integer | 否 | 页码 | 1 |
| size | integer | 否 | 每页大小 | 10 |
| status | string | 否 | 状态筛选 | - |

**请求头:**

| 参数名 | 必填 | 说明 |
|--------|------|------|
| Authorization | 是 | `Bearer {token}` |
| X-User-Id | 是 | 教师ID |

#### 请求示例

```bash
curl -X GET "http://localhost:8082/api/problem/teacher/my?page=1&size=10&status=PUBLISHED" \
  -H "Authorization: Bearer {token}" \
  -H "X-User-Id: 1"
```

#### 响应说明

响应格式与「分页查询题目列表」相同。

---

### 4. 获取题目详情

#### 基本信息

- **接口名称**: 获取题目详情
- **请求方法**: GET
- **请求URL**: `/api/problem/{problemId}`
- **权限要求**: 需登录

#### 请求参数

**路径参数 (Path):**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| problemId | integer | 是 | 题目ID |

**请求头:**

| 参数名 | 必填 | 说明 |
|--------|------|------|
| Authorization | 是 | `Bearer {token}` |

#### 请求示例

```bash
curl -X GET http://localhost:8082/api/problem/1 \
  -H "Authorization: Bearer {token}"
```

#### 响应说明

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

### 5. 更新题目

#### 基本信息

- **接口名称**: 更新题目
- **请求方法**: PUT
- **请求URL**: `/api/problem/{problemId}`
- **权限要求**: 教师(仅自己创建的题目)

#### 请求参数

**路径参数 (Path):**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| problemId | integer | 是 | 题目ID |

**请求头:**

| 参数名 | 必填 | 说明 |
|--------|------|------|
| Content-Type | 是 | 固定值 `application/json` |
| Authorization | 是 | `Bearer {token}` |
| X-User-Id | 是 | 教师ID |

**请求体 (Body):**

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

#### 请求示例

```bash
curl -X PUT http://localhost:8082/api/problem/1 \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 1" \
  -d '{
    "title": "查询年龄大于18岁的用户（更新）",
    "difficulty": "MEDIUM"
  }'
```

#### 响应说明

**成功响应 (200 OK):**

返回更新后的题目信息，格式与创建题目响应相同。

**错误响应:**

| 状态码 | 说明 |
|--------|------|
| 403 | 仅题目创建者可更新 |
| 404 | 题目不存在 |

---

### 6. 删除题目

#### 基本信息

- **接口名称**: 删除题目
- **请求方法**: DELETE
- **请求URL**: `/api/problem/{problemId}`
- **权限要求**: 教师(仅自己创建的题目)

#### 请求参数

**路径参数 (Path):**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| problemId | integer | 是 | 题目ID |

**请求头:**

| 参数名 | 必填 | 说明 |
|--------|------|------|
| Authorization | 是 | `Bearer {token}` |
| X-User-Id | 是 | 教师ID |

#### 请求示例

```bash
curl -X DELETE http://localhost:8082/api/problem/1 \
  -H "Authorization: Bearer {token}" \
  -H "X-User-Id: 1"
```

#### 响应说明

**成功响应 (204 No Content):**

无响应体。

**错误响应:**

| 状态码 | 说明 |
|--------|------|
| 403 | 仅题目创建者可删除 |
| 404 | 题目不存在 |

---

### 7. 更新题目状态

#### 基本信息

- **接口名称**: 更新题目状态
- **请求方法**: PUT
- **请求URL**: `/api/problem/{problemId}/status`
- **权限要求**: 教师(仅自己创建的题目)

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

#### 请求参数

**路径参数 (Path):**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| problemId | integer | 是 | 题目ID |

**请求头:**

| 参数名 | 必填 | 说明 |
|--------|------|------|
| Content-Type | 是 | 固定值 `application/json` |
| Authorization | 是 | `Bearer {token}` |
| X-User-Id | 是 | 教师ID |

**请求体 (Body):**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| status | string | 是 | 新状态 |

#### 请求示例

```bash
curl -X PUT http://localhost:8082/api/problem/1/status \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 1" \
  -d '{
    "status": "PUBLISHED"
  }'
```

#### 响应说明

**成功响应 (200 OK):**

返回更新后的题目信息。

**错误响应:**

| 状态码 | 说明 |
|--------|------|
| 400 | 无效的状态流转 |
| 403 | 仅教师可操作 |

---

### 8. 批量导入题目

#### 基本信息

- **接口名称**: 批量导入题目
- **请求方法**: POST
- **请求URL**: `/api/problem/batch`
- **权限要求**: 教师

#### 请求参数

**请求头:**

| 参数名 | 必填 | 说明 |
|--------|------|------|
| Content-Type | 是 | 固定值 `application/json` |
| Authorization | 是 | `Bearer {token}` |
| X-User-Id | 是 | 教师ID |

**请求体 (Body):**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| problems | array | 是 | 题目列表 |

#### 请求示例

```bash
curl -X POST http://localhost:8082/api/problem/batch \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 1" \
  -d '{
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
  }'
```

#### 响应说明

**成功响应 (201 Created):**

| 字段 | 类型 | 说明 |
|------|------|------|
| successCount | integer | 成功导入数量 |
| failCount | integer | 失败数量 |
| problems | array | 成功导入的题目列表 |
| errors | array | 错误信息列表 |

**响应示例:**

```json
{
  "successCount": 1,
  "failCount": 1,
  "problems": [
    {
      "problemId": 2,
      "title": "题目1",
      ...
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

## 标签管理

### 9. 获取所有标签

#### 基本信息

- **接口名称**: 获取所有标签
- **请求方法**: GET
- **请求URL**: `/api/tag`
- **权限要求**: 需登录

#### 请求示例

```bash
curl -X GET http://localhost:8082/api/tag \
  -H "Authorization: Bearer {token}"
```

#### 响应说明

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

#### 基本信息

- **接口名称**: 创建标签
- **请求方法**: POST
- **请求URL**: `/api/tag`
- **权限要求**: 需登录

#### 请求参数

**请求体 (Body):**

| 参数名 | 类型 | 必填 | 说明 | 默认值 |
|--------|------|------|------|--------|
| name | string | 是 | 标签名称 | - |
| color | string | 否 | 标签颜色 | `#3B82F6` |

#### 请求示例

```bash
curl -X POST http://localhost:8082/api/tag \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "AGGREGATE",
    "color": "#F59E0B"
  }'
```

#### 响应说明

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

#### 基本信息

- **接口名称**: 更新题目标签
- **请求方法**: PUT
- **请求URL**: `/api/problem/{problemId}/tags`
- **权限要求**: 需登录

#### 请求参数

**路径参数 (Path):**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| problemId | integer | 是 | 题目ID |

**请求体 (Body):**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| tagIds | array | 是 | 标签ID列表 |

#### 请求示例

```bash
curl -X PUT http://localhost:8082/api/problem/1/tags \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "tagIds": [1, 2, 3]
  }'
```

#### 响应说明

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

## 数据模型

### ProblemResponse

```typescript
interface ProblemResponse {
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
  tags: TagResponse[];
}
```

### ProblemBasicResponse

```typescript
interface ProblemBasicResponse {
  responseType: 'basic';
  problemId: number;
  title: string;
  description?: string;
  difficulty: 'EASY' | 'MEDIUM' | 'HARD';
  sqlType: 'DQL' | 'DML' | 'DDL' | 'DCL';
  expectedType: 'RESULT_SET' | 'DATA_SNAPSHOT' | 'METADATA' | 'PRIVILEGE';
  teacherId: number;
  createdAt: string;
}
```

### CreateProblemRequest

```typescript
interface CreateProblemRequest {
  title: string;
  description?: string;
  difficulty?: 'EASY' | 'MEDIUM' | 'HARD';
  sqlType: 'DQL' | 'DML' | 'DDL' | 'DCL';
  aiAssisted?: boolean;
  initSql?: string;
  standardAnswer?: string;
  expectedType?: 'RESULT_SET' | 'DATA_SNAPSHOT' | 'METADATA' | 'PRIVILEGE';
}
```

### TagResponse

```typescript
interface TagResponse {
  tagId: number;
  name: string;
  color: string;
  createdAt: string;
}
```

### PageResponse<T>

```typescript
interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
```
