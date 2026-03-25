# SQL 判题引擎 - AI 辅助判题设计

## 1. 概述

AI 辅助判题用于处理传统规则比对难以判断的 SQL 语义等价性问题，特别是在 DDL（数据定义语言）和 DCL（数据控制语言）类型题目中。

## 2. AI 判题判断标准

### 2.1 判断依据

AI 判题依赖以下三个输入：

| 输入 | 来源 | 说明 |
|------|------|------|
| **题目描述** | problem.description | 教师编写的题目要求和约束 |
| **教师标准答案** | problem.standardAnswer | 教师提供的参考 SQL |
| **学生答案** | submission.sqlContent | 学生提交的 SQL |

### 2.2 语义等价性判断

AI 判断的核心是验证**学生答案与教师标准答案是否语义等价**：

- **语义相同** → AI_APPROVED（正确）
- **语义不同** → AI_REJECTED（错误）

### 2.3 语义等价的例子

```sql
-- 教师标准答案
CREATE TABLE users (
    id INT PRIMARY KEY,
    name VARCHAR(50)
);

-- 学生答案 1（等价，列顺序不同）
CREATE TABLE users (
    name VARCHAR(50),
    id INT PRIMARY KEY
);

-- 学生答案 2（等价，关键字大小写不同）
create table users (
    id int primary key,
    name varchar(50)
);

-- 学生答案 3（不等价，缺少 NOT NULL）
CREATE TABLE users (
    id INT PRIMARY KEY,
    name VARCHAR(50)  -- 缺少 NOT NULL
);
```

## 3. 适用场景

### 3.1 DDL 类型题目

| 场景 | 示例 | 传统规则比对 | AI 语义分析 |
|------|------|-------------|------------|
| 表结构等价 | 列顺序不同、关键字大小写 | 困难 | ✓ |
| 索引等价 | INDEX vs KEY | 困难 | ✓ |
| 约束等价 | NOT NULL + DEFAULT vs 仅 DEFAULT | 困难 | ✓ |

### 3.2 DCL 类型题目

| 场景 | 示例 | 传统规则比对 | AI 语义分析 |
|------|------|-------------|------------|
| 权限等价 | 不同的授权语句达到相同效果 | 不可行 | ✓ |
| 用户等价 | CREATE USER vs INSERT + GRANT | 不可行 | ✓ |

### 3.3 不需要 AI 的场景

| SQL 类型 | 判题方式 | 原因 |
|----------|----------|------|
| DQL | 结果集比对 | 结果集有明确的对错标准 |
| DML | 数据快照比对 | 执行后数据状态可精确比对 |

## 4. AI 服务接口

### 4.1 请求格式

```json
{
  "problemDescription": "创建一个用户表，包含ID和姓名，ID为主键，姓名为非空字符串",
  "standardAnswer": "CREATE TABLE users (\n    id INT PRIMARY KEY,\n    name VARCHAR(50) NOT NULL\n);",
  "studentAnswer": "CREATE TABLE users (\n    name VARCHAR(50) NOT NULL,\n    id INT PRIMARY KEY\n);",
  "sqlType": "DDL"
}
```

### 4.2 响应格式

```json
{
  "isCorrect": true,
  "reason": "学生答案与标准答案语义等价，均创建了包含id(主键)和name(非空字符串)两列的用户表",
  "confidence": 0.95,
  "details": {
    "hasIdColumn": true,
    "hasNameColumn": true,
    "idIsPrimaryKey": true,
    "nameIsNotNull": true,
    "columnOrderMatches": false
  }
}
```

### 4.3 错误响应

```json
{
  "isCorrect": false,
  "reason": "学生答案缺少name列的NOT NULL约束，与标准答案不等价",
  "confidence": 0.98,
  "details": {
    "hasIdColumn": true,
    "hasNameColumn": true,
    "idIsPrimaryKey": true,
    "nameIsNotNull": false
  }
}
```

## 5. Prompt 设计

### 5.1 System Prompt

```
你是一个SQL语义分析专家，负责判断学生提交的SQL答案是否与标准答案语义等价。

判断原则：
1. 关注语义而非语法细节
2. 列的顺序不影响语义等价性
3. 关键字大小写不影响语义等价性
4. 空格和换行不影响语义等价性

输出格式：
- isCorrect: boolean - 是否语义等价
- reason: string - 判断理由（中文）
- confidence: float - 判断置信度 (0.0-1.0)
- details: object - 详细分析
```

### 5.2 User Prompt 模板

```
题目描述：
{problemDescription}

教师标准答案：
{standardAnswer}

学生答案：
{studentAnswer}

请分析学生答案是否与标准答案语义等价。
```

## 6. 判题流程

### 6.1 DDL/DCL 判题流程

```
学生提交 SQL
    ↓
判断 SQL 类型
    ↓
┌─────────────────────────────────┐
│ 规则比对（DQL/DML）              │
│   ↓                             │
│ 结果集/数据快照比对              │
│   ↓                             │
│ 匹配？ → 返回 CORRECT/INCORRECT │
└─────────────────────────────────┘
    ↓ (规则比对不确定)
┌─────────────────────────────────┐
│ AI 语义分析（DDL/DCL）          │
│   ↓                             │
│ 构建 Prompt                      │
│   ↓                             │
│ 调用 AI 服务                     │
│   ↓                             │
│ AI 返回判定结果                  │
│   ↓                             │
│ AI_APPROVED / AI_REJECTED       │
└─────────────────────────────────┘
```

### 6.2 AI 辅助触发条件

| 条件 | 说明 |
|------|------|
| SQL 类型为 DDL 或 DCL | 仅这两种类型启用 AI |
| 题目要求 AI 辅助 | problem 表中有 ai辅助标志 |
| 规则比对不确定时 | 可选：规则比对置信度低于阈值时触发 AI |

## 7. 判题结果状态

| 状态 | 说明 | 适用场景 |
|------|------|----------|
| CORRECT | 完全正确 | 所有类型，规则比对成功 |
| INCORRECT | 完全错误 | 所有类型，规则比对失败 |
| AI_APPROVED | AI 判定正确 | DDL/DCL，AI 判断语义等价 |
| AI_REJECTED | AI 判定错误 | DDL/DCL，AI 判断语义不等价 |
| AI_LOW_CONFIDENCE | AI 置信度过低 | DDL/DCL，AI 无法确定，使用兜底策略 |
| AI_FAILED | AI 服务调用失败 | DDL/DCL，AI 服务异常 |
| AI_TIMEOUT | AI 服务超时 | DDL/DCL，AI 服务响应超时 |
| TIME_LIMIT | 执行超时 | 所有类型 |
| ERROR | 系统错误 | 所有类型 |

**状态流转说明**:
- AI 辅助判题时，先调用 AI 服务，根据置信度返回 AI_APPROVED 或 AI_REJECTED
- 当 AI 服务不可用或超时时，使用 fallback 策略（规则比对）
- 高置信度阈值：0.9，低置信度阈值：0.6

## 8. 配置项

### 8.1 应用配置 (application.yml)

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| app.ai.enabled | true | 是否启用 AI 辅助 |
| app.ai.timeout | 30000 | AI 服务调用超时（毫秒） |
| app.ai.retry | 2 | AI 服务调用重试次数 |
| app.ai.confidence-threshold.high | 0.9 | 高置信度阈值 |
| app.ai.confidence-threshold.low | 0.6 | 低置信度阈值 |
| app.ai.api-key | - | AI 服务 API Key |
| app.ai.api-host | - | AI 服务 API 主机地址 |
| app.ai.model | qwen-turbo | AI 模型名称 |

### 8.2 Docker Compose 环境变量

```yaml
services:
  judge-service:
    environment:
      - AI_API_KEY=sk-xxxxxxxxxxxxxxxx
      - AI_API_HOST=ws-xxxxxxxx.cn-beijing.maas.aliyuncs.com
      - AI_MODEL=qwen-turbo
      - AI_ENABLED=true
      - AI_TIMEOUT=30000
      - AI_RETRY=2
```

## 9. 安全考虑

### 9.1 AI 服务隔离

- AI 服务作为内部服务，不暴露给公网
- judge-service 通过内部调用访问 AI 服务
- 使用 API Key 进行内部服务认证

### 9.2 Prompt 注入防护

- 用户输入（学生答案）作为 Prompt 的一部分
- 实施输入长度限制
- 转义特殊字符防止 Prompt 注入

### 9.3 响应验证

- 验证 AI 返回的 JSON 格式
- 校验置信度值范围
- 对高置信度结果进行抽样复核
