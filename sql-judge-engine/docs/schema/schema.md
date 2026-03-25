# SQL 判题引擎 - 数据库 Schema 设计

## 1. 数据库概述

本系统使用两个 MySQL 数据库：

| 数据库 | 用途 | 说明 |
|--------|------|------|
| **business_db** | 业务数据存储 | user-service、problem-service、submission-service、result-service |
| **判题容器内 MySQL** | 学生 SQL 执行目标 | 每个容器有独立 MySQL 实例，题目数据存储在容器内 |

---

## 2. 业务库 Schema

### 2.1 ER 图

```
┌─────────────┐       ┌─────────────┐       ┌─────────────┐
│    users    │       │  problems   │       │ test_cases  │
├─────────────┤       ├─────────────┤       ├─────────────┤
│ id (PK)     │       │ id (PK)     │       │ id (PK)     │
│ username    │       │ title       │◄──────│ problem_id  │
│ password    │       │ description │       │ name        │
│ role        │       │ sql_type    │       │ init_sql    │
│ email       │       │ difficulty  │       │ expected_*  │
│ created_at  │       │ status      │       │ weight      │
└─────────────┘       │ teacher_id  │       └─────────────┘
                      │ created_at  │
                      └─────────────┘
                            │
                            │ N:M
                            ▼
                      ┌─────────────┐       ┌─────────────┐
                      │ problem_tags│       │    tags     │
                      ├─────────────┤       ├─────────────┤
                      │ problem_id  │◄──────│ id (PK)     │
                      │ tag_id     │       │ name        │
                      └─────────────┘       │ color       │
                            │               └─────────────┘
                            ▼
┌─────────────┐       ┌─────────────┐
│submissions  │       │judge_results│
├─────────────┤       ├─────────────┤
│ id (PK)     │──────►│ submission_id│
│ problem_id  │       │ test_case_id │
│ student_id  │       │ score        │
│ sql_content │       │ status       │
│ status      │       │ error_msg    │
│ submitted_at│       │ exec_time    │
└─────────────┘       └─────────────┘
```

---

### 2.2 users 表

用户表，存储所有用户（教师和学生）信息。

```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码(BCrypt加密)',
    role ENUM('TEACHER', 'STUDENT') NOT NULL COMMENT '角色',
    email VARCHAR(100) COMMENT '邮箱',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_username (username),
    INDEX idx_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';
```

**说明**：
- `password` 存储 BCrypt 加密后的密码
- `role` 区分教师和学生，用于权限控制
- 无管理员角色，教师和学生自行注册

---

### 2.3 problems 表

题目表，存储题目基本信息。

```sql
CREATE TABLE problems (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '题目ID',
    title VARCHAR(200) NOT NULL COMMENT '题目标题',
    description TEXT COMMENT '题目描述',
    difficulty ENUM('EASY', 'MEDIUM', 'HARD') DEFAULT 'MEDIUM' COMMENT '难度',
    sql_type ENUM('DQL', 'DML', 'DDL', 'DCL') NOT NULL COMMENT 'SQL类型',
    status ENUM('DRAFT', 'READY', 'PUBLISHED', 'ARCHIVED') DEFAULT 'DRAFT' COMMENT '题目状态',
    ai_assisted BOOLEAN DEFAULT FALSE COMMENT '是否启用AI辅助判题',
    init_sql TEXT COMMENT '初始化SQL脚本(建表、数据等)',
    standard_answer TEXT COMMENT '标准答案SQL(教师用)',
    expected_result TEXT COMMENT '期望结果(JSON格式)',
    expected_type ENUM('RESULT_SET', 'DATA_SNAPSHOT', 'METADATA', 'PRIVILEGE') COMMENT '期望结果类型',
    teacher_id BIGINT NOT NULL COMMENT '创建教师ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_teacher (teacher_id),
    INDEX idx_sql_type (sql_type),
    INDEX idx_difficulty (difficulty),
    INDEX idx_status (status),

    CONSTRAINT fk_problems_teacher FOREIGN KEY (teacher_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='题目表';
```

**说明**：
- `status` 状态流转：DRAFT → READY → PUBLISHED → ARCHIVED
  - DRAFT：草稿状态，可编辑
  - READY：准备就绪，待发布
  - PUBLISHED：已发布，学生可见
  - ARCHIVED：已归档，不再使用
- `ai_assisted` 标记是否启用 AI 辅助判题（主要用于 DDL/DCL 类型题目）
- `init_sql` 存储创建题目时教师提供的初始化脚本
- `standard_answer` 存储标准答案（仅教师可见）
- `expected_result` 存储执行标准答案后的期望结果（JSON格式）
- `expected_type` 定义期望结果类型：RESULT_SET(结果集)、DATA_SNAPSHOT(数据快照)、METADATA(元数据)、PRIVILEGE(权限)
- `teacher_id` 记录题目创建者，限制只有创建者可以修改/删除

---

### 2.4 tags 表

标签表，存储题目标签。

```sql
CREATE TABLE tags (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '标签ID',
    name VARCHAR(50) NOT NULL UNIQUE COMMENT '标签名称',
    color VARCHAR(20) DEFAULT '#3B82F6' COMMENT '标签颜色',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    INDEX idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='标签表';
```

---

### 2.5 problem_tags 表

题目-标签关联表。

```sql
CREATE TABLE problem_tags (
    problem_id BIGINT NOT NULL COMMENT '题目ID',
    tag_id BIGINT NOT NULL COMMENT '标签ID',

    PRIMARY KEY (problem_id, tag_id),

    CONSTRAINT fk_problem_tags_problem FOREIGN KEY (problem_id) REFERENCES problems(id) ON DELETE CASCADE,
    CONSTRAINT fk_problem_tags_tag FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='题目-标签关联表';
```

---

### 2.6 test_cases 表

测试用例表，一个题目可以有多个测试用例。

```sql
CREATE TABLE test_cases (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '测试用例ID',
    problem_id BIGINT NOT NULL COMMENT '所属题目ID',
    name VARCHAR(100) NOT NULL COMMENT '测试用例名称',
    init_sql TEXT COMMENT '该用例的初始化SQL(覆盖题目级别)',
    expected_result TEXT COMMENT '期望结果(JSON格式)',
    expected_type ENUM('RESULT_SET', 'DATA_SNAPSHOT', 'METADATA', 'PRIVILEGE') NOT NULL COMMENT '期望结果类型',
    weight INT DEFAULT 1 COMMENT '权重(用于计算总分)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_problem (problem_id),

    CONSTRAINT fk_test_cases_problem FOREIGN KEY (problem_id) REFERENCES problems(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='测试用例表';
```

**说明**：
- `expected_type` 定义期望结果的结构：
  - `RESULT_SET`：SELECT 结果集
  - `DATA_SNAPSHOT`：DML 操作后的数据快照
  - `METADATA`：DDL 操作后的表结构
  - `PRIVILEGE`：DCL 操作后的权限
- `weight` 用于多测试用例时的加权计分
- ON DELETE CASCADE：删除题目时自动删除关联测试用例

---

### 2.7 submissions 表

提交记录表，记录学生每次提交。

```sql
CREATE TABLE submissions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '提交ID',
    problem_id BIGINT NOT NULL COMMENT '题目ID',
    student_id BIGINT NOT NULL COMMENT '学生ID',
    sql_content TEXT NOT NULL COMMENT '提交的SQL内容',
    status ENUM('PENDING', 'JUDGING', 'SUCCESS', 'FAILED') DEFAULT 'PENDING' COMMENT '状态',
    submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',

    INDEX idx_problem (problem_id),
    INDEX idx_student (student_id),
    INDEX idx_status (status),
    INDEX idx_submitted_at (submitted_at),
    INDEX idx_student_problem (student_id, problem_id),

    CONSTRAINT fk_submissions_problem FOREIGN KEY (problem_id) REFERENCES problems(id),
    CONSTRAINT fk_submissions_student FOREIGN KEY (student_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='提交记录表';
```

**说明**：
- `status` 状态流转：PENDING → JUDGING → SUCCESS/FAILED
- `student_id` 和 `problem_id` 联合索引用于查询某学生在某题目的提交历史

---

### 2.8 judge_results 表

判题结果表，记录每个提交的判题结果。

```sql
CREATE TABLE judge_results (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '结果ID',
    submission_id BIGINT NOT NULL COMMENT '提交ID',
    test_case_id BIGINT COMMENT '测试用例ID(可为NULL)',
    score DECIMAL(5,2) COMMENT '得分',
    status ENUM('CORRECT', 'INCORRECT', 'TIME_LIMIT', 'ERROR', 'AI_APPROVED', 'AI_REJECTED') NOT NULL COMMENT '状态',
    error_message TEXT COMMENT '错误信息(学生可见的通用错误)',
    execution_time_ms BIGINT COMMENT '执行时间(毫秒)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    INDEX idx_submission (submission_id),
    INDEX idx_test_case (test_case_id),
    INDEX idx_status (status),

    CONSTRAINT fk_judge_results_submission FOREIGN KEY (submission_id) REFERENCES submissions(id),
    CONSTRAINT fk_judge_results_test_case FOREIGN KEY (test_case_id) REFERENCES test_cases(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='判题结果表';
```

**说明**：
- `submission_id` + `test_case_id` 联合唯一：每个提交对每个测试用例只有一条结果
- `test_case_id` 可为空：表示总体结果；有值时表示具体测试用例结果
- `error_message` 仅存储学生可见的通用错误，不返回期望vs实际对比
- `status` 状态说明：
  - CORRECT：规则比对正确
  - INCORRECT：规则比对错误
  - TIME_LIMIT：执行超时
  - ERROR：系统错误
  - AI_APPROVED：AI 判定正确（DDL/DCL）
  - AI_REJECTED：AI 判定错误（DDL/DCL）

---

## 3. 判题容器内数据库

### 3.1 设计思路

每个判题容器有独立的 MySQL 实例，学生 SQL 在容器内执行。

#### 题目数据库命名

```
problem_{problem_id}
```

例如：`problem_100` 表示题目ID为100的数据库。

#### 教师出题阶段

1. 教师在 problem-service 创建题目，设定 `init_sql`
2. 系统在**题目设计库**（独立 MySQL 实例）执行初始化 SQL
3. 保存标准答案执行后的状态快照到 `test_cases.expected_result`

#### 学生答题阶段

1. judge-service 从容器池获取容器
2. 在容器 MySQL 创建 `problem_{problem_id}` 数据库
3. 执行题目初始化 SQL
4. 执行学生 SQL
5. 结果比对
6. 容器重置（下一次使用前清空数据）

---

### 3.2 容器 MySQL 配置

```dockerfile
FROM mysql:8.0

ENV MYSQL_ROOT_PASSWORD=root_password
ENV MYSQL_DATABASE=judge_db

COPY init-scripts/ /docker-entrypoint-initdb.d/

EXPOSE 3306
```

**容器特点**：
- MySQL 8.0
- 预装初始化脚本（可选）
- 容器启动后 MySQL 服务自动启动
- 支持通过环境变量配置

---

## 4. 索引优化建议

### 4.1 高频查询优化

| 查询场景 | 索引建议 |
|----------|----------|
| 学生查询自己的提交历史 | `(student_id, submitted_at)` 复合索引 |
| 教师查询自己题目的提交 | `(teacher_id, created_at)` 复合索引 |
| 题目排行榜 | `(problem_id, score)` 复合索引 |
| 按状态查询提交 | `(status, submitted_at)` 复合索引 |

### 4.2 分页优化

提交历史查询使用延迟关联：

```sql
SELECT s.*, j.score, j.status
FROM submissions s
LEFT JOIN judge_results j ON s.id = j.submission_id
WHERE s.student_id = ?
ORDER BY s.submitted_at DESC
LIMIT 100, 20;
```

---

## 5. 数据迁移工具

使用 Flyway 管理数据库版本。

### 5.1 版本目录结构

```
src/main/resources/db/migration/
├── V1__create_users.sql
├── V2__create_problems.sql
├── V3__create_test_cases.sql
├── V4__create_submissions.sql
└── V5__create_judge_results.sql
```

### 5.2 配置

```yaml
spring:
  flyway:
    enabled: true
    baseline-on-migrate: true
    locations: classpath:db/migration
```

---

## 6. 数据字典

### 6.1 枚举值

| 字段 | 值 | 说明 |
|------|-----|------|
| `users.role` | TEACHER | 教师 |
| `users.role` | STUDENT | 学生 |
| `problems.difficulty` | EASY | 简单 |
| `problems.difficulty` | MEDIUM | 中等 |
| `problems.difficulty` | HARD | 困难 |
| `problems.sql_type` | DQL | 数据查询语言 |
| `problems.sql_type` | DML | 数据操作语言 |
| `problems.sql_type` | DDL | 数据定义语言 |
| `problems.sql_type` | DCL | 数据控制语言 |
| `problems.status` | DRAFT | 草稿 |
| `problems.status` | READY | 准备就绪 |
| `problems.status` | PUBLISHED | 已发布 |
| `problems.status` | ARCHIVED | 已归档 |
| `problems.expected_type` | RESULT_SET | SELECT结果集 |
| `problems.expected_type` | DATA_SNAPSHOT | 数据快照 |
| `problems.expected_type` | METADATA | 元数据 |
| `problems.expected_type` | PRIVILEGE | 权限 |
| `test_cases.expected_type` | RESULT_SET | SELECT结果集 |
| `test_cases.expected_type` | DATA_SNAPSHOT | 数据快照 |
| `test_cases.expected_type` | METADATA | 元数据 |
| `test_cases.expected_type` | PRIVILEGE | 权限 |
| `submissions.status` | PENDING | 等待判题 |
| `submissions.status` | JUDGING | 判题中 |
| `submissions.status` | SUCCESS | 判题完成 |
| `submissions.status` | FAILED | 判题失败 |
| `judge_results.status` | CORRECT | 正确 |
| `judge_results.status` | INCORRECT | 错误 |
| `judge_results.status` | TIME_LIMIT | 超时 |
| `judge_results.status` | ERROR | 系统错误 |
| `judge_results.status` | AI_APPROVED | AI判定正确（DDL/DCL） |
| `judge_results.status` | AI_REJECTED | AI判定错误（DDL/DCL） |
| `containers.status` | AVAILABLE | 可用 |
| `containers.status` | IN_USE | 使用中 |
| `containers.status` | ERROR | 错误 |
| `containers.status` | DESTROYED | 已销毁 |
