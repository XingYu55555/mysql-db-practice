# 结果服务 API

> 基础URL: `http://localhost:8086`

## 接口概览

### 判题结果

| 方法 | 路径 | 描述 | 权限 |
|------|------|------|------|
| GET | `/api/result/submission/{submissionId}` | 获取判题结果 | 需登录 |
| GET | `/api/result/student/{studentId}` | 获取学生成绩列表 | 学生(仅自己)/教师 |

### 排行榜

| 方法 | 路径 | 描述 | 权限 |
|------|------|------|------|
| GET | `/api/result/problem/{problemId}/leaderboard` | 获取题目排行榜 | 需登录 |
| GET | `/api/result/leaderboard` | 获取总排行榜 | 需登录 |

### 统计信息

| 方法 | 路径 | 描述 | 权限 |
|------|------|------|------|
| GET | `/api/result/statistics` | 获取判题统计 | 教师 |

### 内部接口

| 方法 | 路径 | 描述 | 权限 |
|------|------|------|------|
| POST | `/api/result` | 创建判题结果 | 内部服务 |

---

## 1. 获取判题结果

### 基本信息

- **接口名称**: 获取判题结果
- **请求方法**: GET
- **请求URL**: `/api/result/submission/{submissionId}`
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
curl -X GET http://localhost:8086/api/result/submission/100 \
  -H "Authorization: Bearer {token}"
```

### 响应说明

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

**响应示例:**

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

## 2. 获取学生成绩列表

### 基本信息

- **接口名称**: 获取学生成绩列表
- **请求方法**: GET
- **请求URL**: `/api/result/student/{studentId}`
- **权限要求**: 学生(仅自己)/教师

### 请求参数

**路径参数 (Path):**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| studentId | integer | 是 | 学生ID |

**查询参数 (Query):**

| 参数名 | 类型 | 必填 | 说明 | 默认值 |
|--------|------|------|------|--------|
| page | integer | 否 | 页码 | 1 |
| size | integer | 否 | 每页大小 | 10 |

**请求头:**

| 参数名 | 必填 | 说明 |
|--------|------|------|
| Authorization | 是 | `Bearer {token}` |

### 请求示例

```bash
# 学生查询自己的成绩
curl -X GET "http://localhost:8086/api/result/student/1?page=1&size=10" \
  -H "Authorization: Bearer {token}"

# 教师查询某个学生的成绩
curl -X GET "http://localhost:8086/api/result/student/2?page=1&size=10" \
  -H "Authorization: Bearer {teacher_token}"
```

### 响应说明

**成功响应 (200 OK):**

| 字段 | 类型 | 说明 |
|------|------|------|
| content | array | 成绩列表 |
| page | integer | 当前页码 |
| size | integer | 每页大小 |
| totalElements | integer | 总记录数 |
| totalPages | integer | 总页数 |

**响应示例:**

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

## 3. 获取题目排行榜

### 基本信息

- **接口名称**: 获取题目排行榜
- **请求方法**: GET
- **请求URL**: `/api/result/problem/{problemId}/leaderboard`
- **权限要求**: 需登录

### 请求参数

**路径参数 (Path):**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| problemId | integer | 是 | 题目ID |

**查询参数 (Query):**

| 参数名 | 类型 | 必填 | 说明 | 默认值 |
|--------|------|------|------|--------|
| page | integer | 否 | 页码 | 1 |
| size | integer | 否 | 每页大小 | 50 |

**请求头:**

| 参数名 | 必填 | 说明 |
|--------|------|------|
| Authorization | 是 | `Bearer {token}` |

### 请求示例

```bash
curl -X GET "http://localhost:8086/api/result/problem/1/leaderboard?page=1&size=10" \
  -H "Authorization: Bearer {token}"
```

### 响应说明

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

**响应示例:**

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

## 4. 获取总排行榜

### 基本信息

- **接口名称**: 获取总排行榜
- **请求方法**: GET
- **请求URL**: `/api/result/leaderboard`
- **权限要求**: 需登录

### 请求参数

**查询参数 (Query):**

| 参数名 | 类型 | 必填 | 说明 | 默认值 |
|--------|------|------|------|--------|
| page | integer | 否 | 页码 | 1 |
| size | integer | 否 | 每页大小 | 50 |

**请求头:**

| 参数名 | 必填 | 说明 |
|--------|------|------|
| Authorization | 是 | `Bearer {token}` |

### 请求示例

```bash
curl -X GET "http://localhost:8086/api/result/leaderboard?page=1&size=20" \
  -H "Authorization: Bearer {token}"
```

### 响应说明

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

**响应示例:**

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

## 5. 获取判题统计

### 基本信息

- **接口名称**: 获取判题统计
- **请求方法**: GET
- **请求URL**: `/api/result/statistics`
- **权限要求**: 教师

### 请求参数

**查询参数 (Query):**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| problemId | integer | 否 | 按题目筛选（不提供则统计全部） |

**请求头:**

| 参数名 | 必填 | 说明 |
|--------|------|------|
| Authorization | 是 | `Bearer {token}` |

### 请求示例

```bash
# 统计全部题目的判题情况
curl -X GET http://localhost:8086/api/result/statistics \
  -H "Authorization: Bearer {teacher_token}"

# 统计某个题目的判题情况
curl -X GET "http://localhost:8086/api/result/statistics?problemId=1" \
  -H "Authorization: Bearer {teacher_token}"
```

### 响应说明

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

**响应示例:**

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

## 6. 创建判题结果（内部接口）

### 基本信息

- **接口名称**: 创建判题结果
- **请求方法**: POST
- **请求URL**: `/api/result`
- **权限要求**: 内部服务（使用API Key）

### 请求参数

**请求头:**

| 参数名 | 必填 | 说明 |
|--------|------|------|
| Content-Type | 是 | 固定值 `application/json` |
| X-API-Key | 是 | 内部服务API Key |

**请求体 (Body):**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| submissionId | integer | 是 | 提交ID |
| problemId | integer | 否 | 题目ID |
| studentId | integer | 否 | 学生ID |
| score | number | 是 | 得分(0-100) |
| status | string | 是 | 判题状态 |
| executionTimeMs | integer | 否 | 执行时间(毫秒) |
| errorMessage | string | 否 | 错误信息 |
| metadata | object | 否 | 额外元数据 |

### 请求示例

```bash
curl -X POST http://localhost:8086/api/result \
  -H "X-API-Key: internal-api-key" \
  -H "Content-Type: application/json" \
  -d '{
    "submissionId": 100,
    "problemId": 1,
    "studentId": 1,
    "score": 100.00,
    "status": "CORRECT",
    "executionTimeMs": 45,
    "errorMessage": null,
    "metadata": {
      "containerId": "abc123",
      "dockerImage": "mysql:8.0"
    }
  }'
```

### 响应说明

**成功响应 (201 Created):**

| 字段 | 类型 | 说明 |
|------|------|------|
| resultId | integer | 结果ID |
| submissionId | integer | 提交ID |
| createdAt | string | 创建时间 |

**响应示例:**

```json
{
  "resultId": 50,
  "submissionId": 100,
  "createdAt": "2024-01-15T10:30:05"
}
```

---

## 数据模型

### ResultDetailResponse

```typescript
interface ResultDetailResponse {
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
```

### LeaderboardEntry

```typescript
interface LeaderboardEntry {
  rank: number;
  studentId: number;
  studentUsername: string;
  bestScore: number;
  latestSubmitTime: string;
}
```

### OverallLeaderboardEntry

```typescript
interface OverallLeaderboardEntry {
  rank: number;
  studentId: number;
  studentUsername: string;
  totalScore: number;
  problemsSolved: number;
  totalSubmissions: number;
}
```

### StatisticsResponse

```typescript
interface StatisticsResponse {
  problemId?: number;
  totalSubmissions: number;
  acceptedSubmissions: number;
  acceptanceRate: number;
  averageScore: number;
  maxScore: number;
  minScore: number;
  averageExecutionTime: number;
}
```

### CreateResultRequest

```typescript
interface CreateResultRequest {
  submissionId: number;
  problemId?: number;
  studentId?: number;
  score: number;
  status: string;
  executionTimeMs?: number;
  errorMessage?: string;
  metadata?: Record<string, any>;
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

### 排行榜组件

```typescript
// api/result.ts
import axios from 'axios';

const API_BASE = 'http://localhost:8086';

export interface ResultDetailResponse {
  resultId: number;
  submissionId: number;
  problemId: number;
  problemTitle: string;
  studentId: number;
  studentUsername: string;
  score: number;
  status: string;
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

export interface StatisticsResponse {
  problemId?: number;
  totalSubmissions: number;
  acceptedSubmissions: number;
  acceptanceRate: number;
  averageScore: number;
  maxScore: number;
  minScore: number;
  averageExecutionTime: number;
}

export const resultApi = {
  // 获取判题结果
  getResultBySubmission(submissionId: number) {
    return axios.get(`${API_BASE}/api/result/submission/${submissionId}`);
  },

  // 获取学生成绩列表
  getStudentResults(studentId: number, params?: { page?: number; size?: number }) {
    return axios.get(`${API_BASE}/api/result/student/${studentId}`, { params });
  },

  // 获取题目排行榜
  getProblemLeaderboard(problemId: number, params?: { page?: number; size?: number }) {
    return axios.get(`${API_BASE}/api/result/problem/${problemId}/leaderboard`, { params });
  },

  // 获取总排行榜
  getOverallLeaderboard(params?: { page?: number; size?: number }) {
    return axios.get(`${API_BASE}/api/result/leaderboard`, { params });
  },

  // 获取统计信息（教师）
  getStatistics(problemId?: number) {
    return axios.get(`${API_BASE}/api/result/statistics`, {
      params: problemId ? { problemId } : undefined
    });
  }
};
```

### Vue 3 排行榜组件示例

```vue
<template>
  <div class="leaderboard">
    <h2>排行榜</h2>
    
    <!-- 切换类型 -->
    <div class="tabs">
      <button 
        :class="{ active: type === 'overall' }"
        @click="type = 'overall'"
      >
        总排行榜
      </button>
      <button 
        :class="{ active: type === 'problem' }"
        @click="type = 'problem'"
      >
        题目排行榜
      </button>
    </div>

    <!-- 题目选择 -->
    <div v-if="type === 'problem'" class="problem-select">
      <select v-model="selectedProblemId">
        <option value="">请选择题目</option>
        <option v-for="p in problems" :key="p.problemId" :value="p.problemId">
          {{ p.title }}
        </option>
      </select>
    </div>

    <!-- 排行榜列表 -->
    <table v-if="entries.length > 0">
      <thead>
        <tr>
          <th>排名</th>
          <th>用户名</th>
          <th v-if="type === 'overall'">总得分</th>
          <th v-if="type === 'overall'">解题数</th>
          <th v-if="type === 'problem'">最高分</th>
          <th>提交时间</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="entry in entries" :key="entry.studentId">
          <td>{{ entry.rank }}</td>
          <td>{{ entry.studentUsername }}</td>
          <td v-if="type === 'overall'">{{ entry.totalScore }}</td>
          <td v-if="type === 'overall'">{{ entry.problemsSolved }}</td>
          <td v-if="type === 'problem'">{{ entry.bestScore }}</td>
          <td>{{ formatDate(entry.latestSubmitTime) }}</td>
        </tr>
      </tbody>
    </table>

    <p v-else class="empty">暂无数据</p>

    <!-- 分页 -->
    <div v-if="totalPages > 1" class="pagination">
      <button 
        :disabled="page === 1"
        @click="page--"
      >
        上一页
      </button>
      <span>{{ page }} / {{ totalPages }}</span>
      <button 
        :disabled="page === totalPages"
        @click="page++"
      >
        下一页
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue';
import { resultApi, LeaderboardEntry, OverallLeaderboardEntry } from '@/api/result';

const type = ref<'overall' | 'problem'>('overall');
const selectedProblemId = ref<number | ''>('');
const entries = ref<(LeaderboardEntry | OverallLeaderboardEntry)[]>([]);
const page = ref(1);
const size = ref(20);
const totalPages = ref(0);
const problems = ref<any[]>([]);

const formatDate = (date: string) => {
  return new Date(date).toLocaleString('zh-CN');
};

const fetchLeaderboard = async () => {
  try {
    if (type.value === 'overall') {
      const response = await resultApi.getOverallLeaderboard({
        page: page.value,
        size: size.value
      });
      entries.value = response.data.entries;
      totalPages.value = response.data.totalPages;
    } else if (selectedProblemId.value) {
      const response = await resultApi.getProblemLeaderboard(
        selectedProblemId.value as number,
        { page: page.value, size: size.value }
      );
      entries.value = response.data.entries;
      totalPages.value = response.data.totalPages;
    }
  } catch (error) {
    console.error('获取排行榜失败:', error);
  }
};

// 监听变化
watch([type, selectedProblemId, page], fetchLeaderboard);

onMounted(() => {
  fetchLeaderboard();
});
</script>
```

### 统计信息组件（教师）

```vue
<template>
  <div class="statistics-panel">
    <h3>判题统计</h3>
    
    <div v-if="statistics" class="stats-grid">
      <div class="stat-card">
        <div class="stat-value">{{ statistics.totalSubmissions }}</div>
        <div class="stat-label">总提交数</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ statistics.acceptedSubmissions }}</div>
        <div class="stat-label">正确提交</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ statistics.acceptanceRate }}%</div>
        <div class="stat-label">通过率</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ statistics.averageScore }}</div>
        <div class="stat-label">平均分</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ statistics.maxScore }}</div>
        <div class="stat-label">最高分</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ statistics.averageExecutionTime }}ms</div>
        <div class="stat-label">平均执行时间</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { resultApi, StatisticsResponse } from '@/api/result';

const props = defineProps<{
  problemId?: number;
}>();

const statistics = ref<StatisticsResponse | null>(null);

onMounted(async () => {
  try {
    const response = await resultApi.getStatistics(props.problemId);
    statistics.value = response.data;
  } catch (error) {
    console.error('获取统计信息失败:', error);
  }
});
</script>
```
