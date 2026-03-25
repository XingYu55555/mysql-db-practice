# 用户服务 API

> 基础URL: `http://localhost:8081`

## 接口概览

| 方法 | 路径 | 描述 | 权限 |
|------|------|------|------|
| POST | `/api/user/register` | 用户注册 | 公开 |
| POST | `/api/user/login` | 用户登录 | 公开 |
| GET | `/api/user/me` | 获取当前用户信息 | 需登录 |
| GET | `/api/user/{userId}` | 根据ID获取用户信息 | 需登录 |

---

## 1. 用户注册

### 基本信息

- **接口名称**: 用户注册
- **请求方法**: POST
- **请求URL**: `/api/user/register`
- **权限要求**: 公开访问

### 请求参数

**请求头:**

| 参数名 | 必填 | 说明 |
|--------|------|------|
| Content-Type | 是 | 固定值 `application/json` |

**请求体 (Body):**

| 参数名 | 类型 | 必填 | 说明 | 约束 |
|--------|------|------|------|------|
| username | string | 是 | 用户名 | 3-50字符 |
| password | string | 是 | 密码 | 6-100字符 |
| role | string | 是 | 用户角色 | `STUDENT` 或 `TEACHER` |
| email | string | 否 | 邮箱 | 有效的邮箱格式 |

### 请求示例

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

```bash
curl -X POST http://localhost:8081/api/user/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "teacher001",
    "password": "password123",
    "role": "TEACHER",
    "email": "teacher@example.com"
  }'
```

### 响应说明

**成功响应 (201 Created):**

| 字段 | 类型 | 说明 |
|------|------|------|
| userId | integer | 用户ID |
| username | string | 用户名 |
| role | string | 用户角色 |

**响应示例:**

```json
{
  "userId": 1,
  "username": "student001",
  "role": "STUDENT"
}
```

**错误响应:**

| 状态码 | 说明 | 示例响应 |
|--------|------|----------|
| 400 | 参数校验失败 | `{"code": 400, "message": "Username must be between 3 and 50 characters", "timestamp": "2024-01-15T10:30:00"}` |
| 409 | 用户已存在 | `{"code": 409, "message": "Username already exists", "timestamp": "2024-01-15T10:30:00"}` |

---

## 2. 用户登录

### 基本信息

- **接口名称**: 用户登录
- **请求方法**: POST
- **请求URL**: `/api/user/login`
- **权限要求**: 公开访问

### 请求参数

**请求头:**

| 参数名 | 必填 | 说明 |
|--------|------|------|
| Content-Type | 是 | 固定值 `application/json` |

**请求体 (Body):**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| username | string | 是 | 用户名 |
| password | string | 是 | 密码 |

### 请求示例

```bash
curl -X POST http://localhost:8081/api/user/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "student001",
    "password": "password123"
  }'
```

### 响应说明

**成功响应 (200 OK):**

| 字段 | 类型 | 说明 |
|------|------|------|
| token | string | JWT Token |
| tokenType | string | Token类型，固定值 `Bearer` |
| expiresIn | integer | Token有效期（秒） |
| userId | integer | 用户ID |
| username | string | 用户名 |
| role | string | 用户角色 |

**响应示例:**

```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxIiwidXNlcm5hbWUiOiJzdHVkZW50MDAxIiwicm9sZSI6IlNUVURFTlQiLCJpYXQiOjE3MDUzMTIyMDAsImV4cCI6MTcwNTM5ODYwMH0...",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "userId": 1,
  "username": "student001",
  "role": "STUDENT"
}
```

**错误响应:**

| 状态码 | 说明 | 示例响应 |
|--------|------|----------|
| 401 | 用户名或密码错误 | `{"code": 401, "message": "Invalid username or password", "timestamp": "2024-01-15T10:30:00"}` |
| 403 | 账号被禁用 | `{"code": 403, "message": "Account is disabled", "timestamp": "2024-01-15T10:30:00"}` |

---

## 3. 获取当前用户信息

### 基本信息

- **接口名称**: 获取当前用户信息
- **请求方法**: GET
- **请求URL**: `/api/user/me`
- **权限要求**: 需登录

### 请求参数

**请求头:**

| 参数名 | 必填 | 说明 |
|--------|------|------|
| Authorization | 是 | `Bearer {token}` |

### 请求示例

```bash
curl -X GET http://localhost:8081/api/user/me \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

### 响应说明

**成功响应 (200 OK):**

| 字段 | 类型 | 说明 |
|------|------|------|
| userId | integer | 用户ID |
| username | string | 用户名 |
| role | string | 用户角色 |
| email | string | 邮箱 |
| createdAt | string | 创建时间（ISO 8601格式） |

**响应示例:**

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

## 4. 根据ID获取用户信息

### 基本信息

- **接口名称**: 根据ID获取用户信息
- **请求方法**: GET
- **请求URL**: `/api/user/{userId}`
- **权限要求**: 需登录

### 请求参数

**路径参数 (Path):**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | integer | 是 | 用户ID |

**请求头:**

| 参数名 | 必填 | 说明 |
|--------|------|------|
| Authorization | 是 | `Bearer {token}` |

### 请求示例

```bash
curl -X GET http://localhost:8081/api/user/1 \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

### 响应说明

**成功响应 (200 OK):**

响应格式与 `/api/user/me` 相同。

**响应示例:**

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
| 404 | 用户不存在 |

---

## 数据模型

### UserInfo

```typescript
interface UserInfo {
  userId: number;        // 用户ID
  username: string;      // 用户名
  role: 'STUDENT' | 'TEACHER';  // 用户角色
  email?: string;        // 邮箱（可选）
  createdAt: string;     // 创建时间，ISO 8601格式
}
```

### LoginResponse

```typescript
interface LoginResponse {
  token: string;         // JWT Token
  tokenType: string;     // Token类型，固定值 "Bearer"
  expiresIn: number;     // Token有效期（秒）
  userId: number;        // 用户ID
  username: string;      // 用户名
  role: string;          // 用户角色
}
```

### RegisterRequest

```typescript
interface RegisterRequest {
  username: string;      // 用户名，3-50字符
  password: string;      // 密码，6-100字符
  role: 'STUDENT' | 'TEACHER';  // 用户角色
  email?: string;        // 邮箱（可选）
}
```

### LoginRequest

```typescript
interface LoginRequest {
  username: string;      // 用户名
  password: string;      // 密码
}
```

---

## 前端使用示例

### Vue 3 + Axios 示例

```typescript
// api/user.ts
import axios from 'axios';

const API_BASE = 'http://localhost:8081';

export interface RegisterRequest {
  username: string;
  password: string;
  role: 'STUDENT' | 'TEACHER';
  email?: string;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface UserInfo {
  userId: number;
  username: string;
  role: string;
  email?: string;
  createdAt: string;
}

export const userApi = {
  // 注册
  register(data: RegisterRequest) {
    return axios.post(`${API_BASE}/api/user/register`, data);
  },

  // 登录
  login(data: LoginRequest) {
    return axios.post(`${API_BASE}/api/user/login`, data);
  },

  // 获取当前用户信息
  getCurrentUser() {
    return axios.get(`${API_BASE}/api/user/me`);
  },

  // 根据ID获取用户信息
  getUserById(userId: number) {
    return axios.get(`${API_BASE}/api/user/${userId}`);
  }
};
```

### 登录页面示例

```vue
<template>
  <div class="login-page">
    <h2>登录</h2>
    <form @submit.prevent="handleLogin">
      <div>
        <label>用户名:</label>
        <input v-model="form.username" type="text" required />
      </div>
      <div>
        <label>密码:</label>
        <input v-model="form.password" type="password" required />
      </div>
      <button type="submit" :disabled="loading">
        {{ loading ? '登录中...' : '登录' }}
      </button>
    </form>
    <p v-if="error" class="error">{{ error }}</p>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { userApi } from '@/api/user';

const router = useRouter();
const loading = ref(false);
const error = ref('');

const form = reactive({
  username: '',
  password: ''
});

const handleLogin = async () => {
  loading.value = true;
  error.value = '';

  try {
    const response = await userApi.login(form);
    const { token, userId, role } = response.data;

    // 存储登录信息
    localStorage.setItem('token', token);
    localStorage.setItem('userId', String(userId));
    localStorage.setItem('role', role);

    // 跳转到首页
    router.push('/');
  } catch (err: any) {
    error.value = err.response?.data?.message || '登录失败';
  } finally {
    loading.value = false;
  }
};
</script>
```
