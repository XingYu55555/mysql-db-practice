# 提交服务 API

> 基础URL: `http://localhost:8083`

## 接口概览

| 方法 | 路径 | 描述 | 权限 |
|------|------|------|------|
| POST | `/api/submission` | 提交SQL答案 | 学生 |
| GET | `/api/submission` | 分页查询提交历史 | 学生(仅自己) |
| GET | `/api/submission/{submissionId}` | 获取提交详情 | 需登录 |
| GET | `/api/submission/{submissionId}/status` | 查询判题状态 | 需登录 |

---

## 1. 提交SQL答案

### 基本信息

- **接口名称**: 提交SQL答案
- **请求方法**: POST
- **请求URL**: `/api/submission`
- **权限要求**: 学生

### 请求参数

**请求头:**

| 参数名 | 必填 | 说明 |
|--------|------|------|
| Content-Type | 是 | 固定值 `application/json` |
| Authorization | 是 | `Bearer {token}` |
| X-User-Id | 是 | 学生ID（网关自动添加） |

**请求体 (Body):**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| problemId | integer | 是 | 题目ID |
| sqlContent | string | 是 | SQL答案内容 |

### 请求示例

```bash
curl -X POST http://localhost:8083/api/submission \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 1" \
  -d '{
    "problemId": 1,
    "sqlContent": "SELECT name, age FROM users WHERE age > 18"
  }'
```

### 响应说明

**成功响应 (202 Accepted):**

提交成功，已进入判题队列。

| 字段 | 类型 | 说明 |
|------|------|------|
| submissionId | integer | 提交ID |
| problemId | integer | 题目ID |
| problemTitle | string | 题目标题 |
| status | string | 提交状态 |
| submittedAt | string | 提交时间 |

**响应示例:**

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
| 404 | 题目不存在 |
| 503 | 判题服务不可用 |

---

## 2. 分页查询提交历史

### 基本信息

- **接口名称**: 分页查询提交历史
- **请求方法**: GET
- **请求URL**: `/api/submission`
- **权限要求**: 学生(仅查询自己的提交)

### 请求参数

**查询参数 (Query):**

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

**请求头:**

| 参数名 | 必填 | 说明 |
|--------|------|------|
| Authorization | 是 | `Bearer {token}` |
| X-User-Id | 是 | 学生ID |

### 请求示例

```bash
# 基础查询
curl -X GET "http://localhost:8083/api/submission?page=1&size=10" \
  -H "Authorization: Bearer {token}" \
  -H "X-User-Id: 1"

# 带筛选条件
curl -X GET "http://localhost:8083/api/submission?problemId=1&status=SUCCESS&sort=score,desc" \
  -H "Authorization: Bearer {token}" \
  -H "X-User-Id: 1"
```

### 响应说明

**成功响应 (200 OK):**

| 字段 | 类型 | 说明 |
|------|------|------|
| content | array | 提交记录列表 |
| page | integer | 当前页码 |
| size | integer | 每页大小 |
| totalElements | integer | 总记录数 |
| totalPages | integer | 总页数 |

**响应示例:**

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

## 3. 获取提交详情

### 基本信息

- **接口名称**: 获取提交详情
- **请求方法**: GET
- **请求URL**: `/api/submission/{submissionId}`
- **权限要求**: 需登录

### 请求参数

**路径参数 (Path):**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| submissionId | integer | 是 | 提交ID |

**请求头:**

| 参数名 | 必填 | 说明 |
|--------|------|------|
| Authorization | 是 | `Bearer {token}` |

### 请求示例

```bash
curl -X GET http://localhost:8083/api/submission/100 \
  -H "Authorization: Bearer {token}"
```

### 响应说明

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

**响应示例:**

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

## 4. 查询判题状态

### 基本信息

- **接口名称**: 查询判题状态
- **请求方法**: GET
- **请求URL**: `/api/submission/{submissionId}/status`
- **权限要求**: 需登录

### 请求参数

**路径参数 (Path):**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| submissionId | integer | 是 | 提交ID |

**请求头:**

| 参数名 | 必填 | 说明 |
|--------|------|------|
| Authorization | 是 | `Bearer {token}` |

### 请求示例

```bash
curl -X GET http://localhost:8083/api/submission/100/status \
  -H "Authorization: Bearer {token}"
```

### 响应说明

**成功响应 (200 OK):**

| 字段 | 类型 | 说明 |
|------|------|------|
| submissionId | integer | 提交ID |
| status | string | 提交状态 |
| score | number | 得分(0-100，判题完成后有值) |
| judgeStatus | string | 判题状态(判题完成后有值) |

**响应示例:**

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

## 数据模型

### SubmissionResponse

```typescript
interface SubmissionResponse {
  submissionId: number;
  problemId: number;
  problemTitle: string;
  status: 'PENDING' | 'JUDGING' | 'SUCCESS' | 'FAILED';
  submittedAt: string;
}
```

### SubmissionDetailResponse

```typescript
interface SubmissionDetailResponse {
  submissionId: number;
  problemId: number;
  problemTitle: string;
  sqlContent: string;
  status: 'PENDING' | 'JUDGING' | 'SUCCESS' | 'FAILED';
  score?: number;
  executionTimeMs?: number;
  judgeStatus?: 'CORRECT' | 'INCORRECT' | 'TIME_LIMIT' | 'ERROR' | 'AI_APPROVED' | 'AI_REJECTED';
  errorMessage?: string;
  submittedAt: string;
}
```

### SubmissionStatusResponse

```typescript
interface SubmissionStatusResponse {
  submissionId: number;
  status: 'PENDING' | 'JUDGING' | 'SUCCESS' | 'FAILED';
  score?: number;
  judgeStatus?: 'CORRECT' | 'INCORRECT' | 'TIME_LIMIT' | 'ERROR' | 'AI_APPROVED' | 'AI_REJECTED';
}
```

### CreateSubmissionRequest

```typescript
interface CreateSubmissionRequest {
  problemId: number;
  sqlContent: string;
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

---

## 前端使用示例

### 提交答案并轮询状态

```typescript
// api/submission.ts
import axios from 'axios';

const API_BASE = 'http://localhost:8083';

export interface CreateSubmissionRequest {
  problemId: number;
  sqlContent: string;
}

export interface SubmissionDetailResponse {
  submissionId: number;
  problemId: number;
  problemTitle: string;
  sqlContent: string;
  status: 'PENDING' | 'JUDGING' | 'SUCCESS' | 'FAILED';
  score?: number;
  executionTimeMs?: number;
  judgeStatus?: string;
  errorMessage?: string;
  submittedAt: string;
}

export const submissionApi = {
  // 提交答案
  createSubmission(data: CreateSubmissionRequest) {
    return axios.post(`${API_BASE}/api/submission`, data);
  },

  // 查询提交历史
  listSubmissions(params?: {
    page?: number;
    size?: number;
    problemId?: number;
    status?: string;
    sort?: string;
  }) {
    return axios.get(`${API_BASE}/api/submission`, { params });
  },

  // 获取提交详情
  getSubmissionDetail(submissionId: number) {
    return axios.get(`${API_BASE}/api/submission/${submissionId}`);
  },

  // 查询判题状态
  getSubmissionStatus(submissionId: number) {
    return axios.get(`${API_BASE}/api/submission/${submissionId}/status`);
  }
};
```

### Vue 3 提交组件示例

```vue
<template>
  <div class="submission-panel">
    <h3>提交答案</h3>
    
    <textarea
      v-model="sqlContent"
      placeholder="在此输入你的SQL答案..."
      rows="10"
      :disabled="isSubmitting"
    />
    
    <button 
      @click="handleSubmit" 
      :disabled="isSubmitting || !sqlContent.trim()"
    >
      {{ isSubmitting ? '提交中...' : '提交' }}
    </button>

    <!-- 判题状态 -->
    <div v-if="submissionStatus" class="status-panel">
      <p>状态: {{ getStatusText(submissionStatus.status) }}</p>
      <p v-if="submissionStatus.score !== undefined">
        得分: {{ submissionStatus.score }}分
      </p>
      <p v-if="submissionStatus.judgeStatus">
        判题结果: {{ getJudgeStatusText(submissionStatus.judgeStatus) }}
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { submissionApi } from '@/api/submission';

const props = defineProps<{
  problemId: number;
}>();

const sqlContent = ref('');
const isSubmitting = ref(false);
const submissionStatus = ref<any>(null);
let pollInterval: number | null = null;

const getStatusText = (status: string) => {
  const map: Record<string, string> = {
    PENDING: '待处理',
    JUDGING: '判题中',
    SUCCESS: '判题完成',
    FAILED: '判题失败'
  };
  return map[status] || status;
};

const getJudgeStatusText = (status: string) => {
  const map: Record<string, string> = {
    CORRECT: '正确',
    INCORRECT: '错误',
    TIME_LIMIT: '超时',
    ERROR: '执行错误',
    AI_APPROVED: 'AI审核通过',
    AI_REJECTED: 'AI审核拒绝'
  };
  return map[status] || status;
};

// 轮询判题状态
const startPolling = (submissionId: number) => {
  if (pollInterval) {
    clearInterval(pollInterval);
  }

  pollInterval = window.setInterval(async () => {
    try {
      const response = await submissionApi.getSubmissionStatus(submissionId);
      submissionStatus.value = response.data;

      // 判题完成或失败，停止轮询
      if (['SUCCESS', 'FAILED'].includes(response.data.status)) {
        if (pollInterval) {
          clearInterval(pollInterval);
          pollInterval = null;
        }
        isSubmitting.value = false;
      }
    } catch (error) {
      console.error('轮询状态失败:', error);
    }
  }, 1000); // 每秒查询一次
};

const handleSubmit = async () => {
  if (!sqlContent.value.trim()) return;

  isSubmitting.value = true;
  submissionStatus.value = null;

  try {
    const response = await submissionApi.createSubmission({
      problemId: props.problemId,
      sqlContent: sqlContent.value
    });

    const { submissionId, status } = response.data;
    submissionStatus.value = { status };

    // 开始轮询判题状态
    startPolling(submissionId);
  } catch (error: any) {
    alert(error.response?.data?.message || '提交失败');
    isSubmitting.value = false;
  }
};
</script>
```

---

## 判题流程说明

```
┌─────────┐    提交SQL    ┌─────────────────┐
│  学生   │ ───────────> │ Submission Service│
│         │              │                 │
│         │ <─────────── │  返回submissionId │
│         │   202 Accepted │  状态: PENDING   │
└─────────┘              └────────┬────────┘
                                  │
                                  │ 投递到消息队列
                                  ▼
                         ┌─────────────────┐
                         │   Message Queue │
                         │    (RabbitMQ)   │
                         └────────┬────────┘
                                  │
                                  │ 消费消息
                                  ▼
                         ┌─────────────────┐
                         │  Judge Service  │
                         │    执行判题      │
                         └────────┬────────┘
                                  │
                                  │ 回调写入结果
                                  ▼
                         ┌─────────────────┐
                         │  Result Service │
                         │   存储判题结果   │
                         └─────────────────┘
```

### 状态流转

```
PENDING -> JUDGING -> SUCCESS/FAILED
```

1. **PENDING**: 提交已接收，等待进入判题队列
2. **JUDGING**: 正在执行判题（运行SQL、比对结果）
3. **SUCCESS**: 判题完成，可以查询得分和详细结果
4. **FAILED**: 判题过程出错（系统错误，非答案错误）
