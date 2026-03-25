# SQL Judge Engine API 文档

> 面向前端开发者的完整API文档

## 目录

- [认证说明](./authentication.md) - JWT认证和权限说明
- [用户服务API](./user-api.md) - 用户注册、登录、信息管理
- [题目服务API](./problem-api.md) - 题目CRUD、标签管理
- [提交服务API](./submission-api.md) - SQL提交、判题状态查询
- [结果服务API](./result-api.md) - 成绩查询、排行榜、统计

## 服务地址

| 服务 | 端口 | 基础URL |
|------|------|---------|
| User Service | 8081 | `http://localhost:8081` |
| Problem Service | 8082 | `http://localhost:8082` |
| Submission Service | 8083 | `http://localhost:8083` |
| Result Service | 8086 | `http://localhost:8086` |

## 统一响应格式

### 成功响应

所有API的成功响应都直接返回数据对象，HTTP状态码表示操作结果：

- `200 OK` - 请求成功
- `201 Created` - 创建成功
- `202 Accepted` - 已接受，异步处理中
- `204 No Content` - 删除成功，无返回内容

### 错误响应

错误响应使用统一的格式：

```json
{
  "code": 400,
  "message": "请求参数错误",
  "timestamp": "2024-01-15T10:30:00"
}
```

### 分页响应

列表查询接口使用统一的分页格式：

```json
{
  "content": [...],
  "page": 1,
  "size": 10,
  "totalElements": 100,
  "totalPages": 10
}
```

## 权限角色

| 角色 | 说明 | 权限 |
|------|------|------|
| `STUDENT` | 学生 | 查看题目、提交答案、查看自己的成绩 |
| `TEACHER` | 教师 | 创建题目、管理题目、查看所有学生成绩、导出报告 |

## 常用HTTP状态码

| 状态码 | 说明 | 场景 |
|--------|------|------|
| 200 | 成功 | GET、PUT请求成功 |
| 201 | 创建成功 | POST创建资源成功 |
| 202 | 已接受 | 提交成功，异步处理中 |
| 204 | 无内容 | DELETE删除成功 |
| 400 | 请求参数错误 | 参数校验失败 |
| 401 | 未授权 | Token缺失或过期 |
| 403 | 禁止访问 | 权限不足 |
| 404 | 资源不存在 | 题目/用户/提交不存在 |
| 409 | 资源冲突 | 用户已存在 |
| 503 | 服务不可用 | 判题服务不可用 |

## 快速开始

### 1. 用户注册

```bash
curl -X POST http://localhost:8081/api/user/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "student001",
    "password": "password123",
    "role": "STUDENT",
    "email": "student@example.com"
  }'
```

### 2. 用户登录

```bash
curl -X POST http://localhost:8081/api/user/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "student001",
    "password": "password123"
  }'
```

### 3. 获取题目列表

```bash
curl -X GET "http://localhost:8082/api/problem?page=1&size=10" \
  -H "Authorization: Bearer {token}"
```

### 4. 提交SQL答案

```bash
curl -X POST http://localhost:8083/api/submission \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "problemId": 1,
    "sqlContent": "SELECT * FROM users WHERE age > 18"
  }'
```

## OpenAPI规范文件

本项目同时提供OpenAPI 3.0规范的YAML文件：

- [user-service.yaml](./user-service.yaml)
- [problem-service.yaml](./problem-service.yaml)
- [submission-service.yaml](./submission-service.yaml)
- [result-service.yaml](./result-service.yaml)

可以使用Swagger UI或Postman导入这些文件进行API测试。

## 注意事项

1. **认证头**: 需要认证的接口必须在请求头中携带 `Authorization: Bearer {token}`
2. **用户ID头**: 部分接口需要 `X-User-Id` 请求头（网关会自动添加）
3. **时区**: 所有时间字段使用ISO 8601格式，时区为UTC
4. **分页**: 页码从1开始，默认每页10条
