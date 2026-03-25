# API 设计 Skill

## 概述

本 Skill 定义了 SQL 判题引擎的 API 设计规范，包括 OpenAPI 规范编写、API 版本管理、错误处理规范、认证授权等。

## 设计原则

### RESTful API 设计原则

1. **资源导向**: API 以资源为中心，使用名词而非动词
2. **HTTP 动词**: 使用标准 HTTP 方法（GET/POST/PUT/DELETE）
3. **状态码**: 使用标准 HTTP 状态码
4. **无状态**: 每个请求都包含认证信息

### 路径设计

```
# 正确示例
GET    /api/problems          # 获取题目列表
POST   /api/problems          # 创建题目
GET    /api/problems/{id}     # 获取题目详情
PUT    /api/problems/{id}     # 更新题目
DELETE /api/problems/{id}     # 删除题目

# 错误示例（使用了动词）
POST /api/createProblem
POST /api/getProblem
```

### 版本管理

使用 URL 路径版本：

```
/api/v1/problems
/api/v2/problems
```

## OpenAPI 规范结构

### 完整示例

```yaml
openapi: 3.0.3
info:
  title: User Service API
  description: 用户管理服务 API
  version: 1.0.0
  contact:
    name: SQL Judge Team

servers:
  - url: http://localhost:8081
    description: 本地开发服务器
  - url: https://api.sqljudge.com
    description: 生产服务器

tags:
  - name: Auth
    description: 认证相关接口
  - name: User
    description: 用户信息接口

paths:
  /api/user/register:
    post:
      tags:
        - Auth
      summary: 用户注册
      description: 教师或学生自行注册
      operationId: register
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/RegisterRequest'
      responses:
        '201':
          description: 注册成功
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/RegisterResponse'
        '409':
          description: 用户已存在
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'

components:
  securitySchemes:
    BearerAuth:
      type: http
      scheme: bearer
      bearerFormat: JWT

  schemas:
    RegisterRequest:
      type: object
      required:
        - username
        - password
        - role
      properties:
        username:
          type: string
          minLength: 3
          maxLength: 50
        password:
          type: string
          minLength: 6
          maxLength: 100
        role:
          type: string
          enum: [TEACHER, STUDENT]

    ErrorResponse:
      type: object
      properties:
        code:
          type: integer
          description: 错误码
        message:
          type: string
          description: 错误信息
        timestamp:
          type: string
          format: date-time
```

## 数据类型规范

### OpenAPI 数据类型

| JSON Type | OpenAPI Type | Format | 说明 |
|-----------|--------------|--------|------|
| string | string | - | 字符串 |
| string | string | date-time | 日期时间 |
| string | string | date | 日期 |
| string | string | email | 邮箱 |
| string | string | uuid | UUID |
| number | number | double | 浮点数 |
| number | number | float | 浮点数 |
| integer | integer | int32 | 32位整数 |
| integer | integer | int64 | 64位整数 |
| boolean | boolean | - | 布尔值 |

### 自定义类型

```yaml
components:
  schemas:
    UserRole:
      type: string
      enum: [TEACHER, STUDENT]

    SubmissionStatus:
      type: string
      enum: [PENDING, JUDGING, SUCCESS, FAILED]

    JudgeStatus:
      type: string
      enum: [CORRECT, INCORRECT, TIME_LIMIT, ERROR]
```

## 认证与授权

### JWT Bearer Token

```yaml
components:
  securitySchemes:
    BearerAuth:
      type: http
      scheme: bearer
      bearerFormat: JWT

paths:
  /api/user/me:
    get:
      security:
        - BearerAuth: []
```

### 角色权限

| 角色 | 可访问接口 |
|------|-----------|
| 未认证 | /api/user/register, /api/user/login |
| STUDENT | /api/submission/**, /api/result/student/{id} |
| TEACHER | /api/problem/**, /api/result/problem/{id}/** |

## 错误处理规范

### HTTP 状态码使用

| 状态码 | 使用场景 |
|--------|----------|
| 200 OK | GET/PUT 成功 |
| 201 Created | POST 创建成功 |
| 202 Accepted | 异步任务已接受 |
| 204 No Content | DELETE 成功 |
| 400 Bad Request | 参数校验失败 |
| 401 Unauthorized | 未认证 |
| 403 Forbidden | 无权限 |
| 404 Not Found | 资源不存在 |
| 409 Conflict | 资源冲突（如用户名已存在） |
| 500 Internal Server Error | 服务器内部错误 |

### 错误响应格式

```yaml
ErrorResponse:
  type: object
  required:
    - code
    - message
  properties:
    code:
      type: integer
      description: |
        错误码:
        1001 - USERNAME_EXISTS
        1002 - INVALID_PASSWORD
        2001 - PROBLEM_NOT_FOUND
        3001 - SUBMISSION_NOT_FOUND
    message:
      type: string
      description: 错误信息（用户可读）
    timestamp:
      type: string
      format: date-time
    details:
      type: array
      items:
        $ref: '#/components/schemas/FieldError'
      description: 字段级错误详情

FieldError:
  type: object
  properties:
    field:
      type: string
    message:
      type: string
```

## 分页规范

### 分页请求参数

```yaml
paths:
  /api/problems:
    get:
      parameters:
        - name: page
          in: query
          schema:
            type: integer
            default: 1
            minimum: 1
        - name: size
          in: query
          schema:
            type: integer
            default: 10
            minimum: 1
            maximum: 100
        - name: sort
          in: query
          schema:
            type: string
            default: createdAt,desc
```

### 分页响应格式

```yaml
ProblemListResponse:
  type: object
  properties:
    content:
      type: array
      items:
        $ref: '#/components/schemas/ProblemResponse'
    page:
      type: integer
      description: 当前页码
    size:
      type: integer
      description: 每页大小
    totalElements:
      type: integer
      description: 总记录数
    totalPages:
      type: integer
      description: 总页数
    hasNext:
      type: boolean
      description: 是否有下一页
    hasPrevious:
      type: boolean
      description: 是否有上一页
```

## 请求/响应规范

### 请求体规范

```yaml
CreateProblemRequest:
  type: object
  required:
    - title
    - sqlType
    - testCases
  properties:
    title:
      type: string
      minLength: 1
      maxLength: 200
    description:
      type: string
    difficulty:
      type: string
      enum: [EASY, MEDIUM, HARD]
      default: MEDIUM
    sqlType:
      type: string
      enum: [DQL, DML, DDL, DCL]
    initSql:
      type: string
      description: 初始化SQL脚本
    testCases:
      type: array
      minItems: 1
      items:
        $ref: '#/components/schemas/TestCaseInput'
```

### 响应体规范

```yaml
ProblemResponse:
  type: object
  properties:
    problemId:
      type: integer
      format: int64
      description: 题目ID
    title:
      type: string
    description:
      type: string
    difficulty:
      type: string
    sqlType:
      type: string
    teacherId:
      type: integer
      format: int64
    createdAt:
      type: string
      format: date-time
```

## API 评审清单

在 API 设计完成后，使用以下清单进行评审：

### 基本规范

- [ ] API 路径使用名词而非动词
- [ ] 使用正确的 HTTP 方法
- [ ] 返回合适的状态码
- [ ] 所有请求参数都有描述
- [ ] 所有响应字段都有描述

### 安全性

- [ ] 需要认证的 API 配置了 BearerAuth
- [ ] 敏感信息不在响应中返回
- [ ] 密码不会在响应中返回

### 可用性

- [ ] 支持分页（如适用）
- [ ] 支持排序（如适用）
- [ ] 错误信息有明确说明
- [ ] 有 summary 和 description

### 一致性

- [ ] 命名风格一致
- [ ] 错误码格式一致
- [ ] 时间格式统一使用 ISO 8601

## 常见问题

### Q: 如何处理文件上传？

```yaml
paths:
  /api/problem/import:
    post:
      requestBody:
        content:
          multipart/form-data:
            schema:
              type: object
              properties:
                file:
                  type: string
                  format: binary
                problemType:
                  type: string
```

### Q: 如何处理数组参数？

```yaml
paths:
  /api/submissions/batch:
    post:
      requestBody:
        content:
          application/json:
            schema:
              type: object
              properties:
                submissionIds:
                  type: array
                  items:
                    type: integer
                    format: int64
```

### Q: 如何实现异步 API？

```yaml
paths:
  /api/submission:
    post:
      responses:
        '202':
          description: 提交已接受，正在判题
          content:
            application/json:
              schema:
                type: object
                properties:
                  submissionId:
                    type: integer
                    format: int64
                  status:
                    type: string
                    example: PENDING
                  pollUrl:
                    type: string
                    description: 用于轮询状态的 URL
```

## 工具推荐

- **Swagger Editor**: https://editor.swagger.io/ - 在线 OpenAPI 编辑器
- **Stoplight**: 桌面 API 设计工具
- **Postman**: API 测试
- **Insomnia**: API 测试
