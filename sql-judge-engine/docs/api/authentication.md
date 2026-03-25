# 认证说明

## JWT认证

SQL Judge Engine 使用 JWT (JSON Web Token) 进行用户认证。

### 认证流程

```
┌─────────┐         ┌──────────────┐         ┌─────────┐
│  前端   │ ──────> │  User Service│ ──────> │  数据库  │
│         │ 登录    │  /api/login  │         │         │
│         │ <────── │              │ <────── │         │
│         │  Token  │              │         │         │
│         │         │              │         │         │
│         │ ──────> │   API Gateway│         │         │
│         │ 请求    │   (验证Token)│         │         │
│         │ <────── │              │         │         │
│         │ 响应    │              │         │         │
└─────────┘         └──────────────┘         └─────────┘
```

### 登录获取Token

**请求:**

```http
POST /api/user/login
Content-Type: application/json

{
  "username": "student001",
  "password": "password123"
}
```

**响应:**

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

### 使用Token

在需要认证的请求中，在请求头中添加 `Authorization` 字段：

```http
GET /api/problem
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Token有效期

- 默认有效期: **24小时** (86400秒)
- Token过期后需要重新登录获取新Token

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
| 导出成绩报告 | ✓ (仅自己) | ✓ (任意学生) |
| 查看统计信息 | ✗ | ✓ |

## 特殊请求头

### X-User-Id

部分接口需要 `X-User-Id` 请求头来标识当前用户身份。这个头通常由API网关自动添加，前端无需手动设置。

需要此头的接口：
- `POST /api/problem` - 创建题目
- `GET /api/problem/teacher/my` - 获取教师的题目列表
- `PUT /api/problem/{id}` - 更新题目
- `DELETE /api/problem/{id}` - 删除题目
- `PUT /api/problem/{id}/status` - 更新题目状态
- `POST /api/problem/batch` - 批量导入题目
- `POST /api/submission` - 提交答案
- `GET /api/submission` - 查询提交历史

## 错误处理

### 401 未授权

当Token缺失、过期或无效时返回：

```json
{
  "code": 401,
  "message": "未授权或Token已过期",
  "timestamp": "2024-01-15T10:30:00"
}
```

**处理方式:**
- 检查Token是否存在
- 检查Token是否过期，过期则重新登录
- 检查Token格式是否正确 (Bearer token)

### 403 禁止访问

当用户权限不足时返回：

```json
{
  "code": 403,
  "message": "仅教师可执行此操作",
  "timestamp": "2024-01-15T10:30:00"
}
```

**处理方式:**
- 检查用户角色是否有权限执行此操作
- 学生尝试访问教师接口时会出现此错误

## 前端实现建议

### 1. Token存储

推荐使用 `localStorage` 或 `sessionStorage` 存储Token：

```javascript
// 登录成功后存储Token
localStorage.setItem('token', response.data.token);
localStorage.setItem('userId', response.data.userId);
localStorage.setItem('role', response.data.role);

// 获取Token
const token = localStorage.getItem('token');
```

### 2. 请求拦截器

使用Axios拦截器自动添加认证头：

```javascript
import axios from 'axios';

// 请求拦截器
axios.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 响应拦截器
axios.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Token过期，清除存储并跳转到登录页
      localStorage.removeItem('token');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);
```

### 3. 权限控制

根据用户角色控制界面显示：

```javascript
// 检查用户角色
const userRole = localStorage.getItem('role');
const isTeacher = userRole === 'TEACHER';
const isStudent = userRole === 'STUDENT';

// Vue/React 中根据角色显示/隐藏元素
{isTeacher && <CreateProblemButton />}
{isStudent && <SubmitAnswerButton />}
```

### 4. 登录状态检查

```javascript
// 检查是否已登录
function isAuthenticated() {
  return !!localStorage.getItem('token');
}

// 路由守卫
router.beforeEach((to, from, next) => {
  if (to.meta.requiresAuth && !isAuthenticated()) {
    next('/login');
  } else {
    next();
  }
});
```
