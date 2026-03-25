# SQL Judge Engine - Black Box Test Scripts

## 概述

本目录包含用于测试 SQL 判题引擎微服务架构的黑盒测试脚本。这些脚本通过 HTTP API 进行测试，不依赖服务内部实现。

## 目录结构

```
scripts/
├── config.sh              # 配置文件（服务URL、测试用户等）
├── common.sh              # 通用函数库
├── check-health.sh        # 健康检查脚本
├── start-services.sh      # 微服务启动脚本
├── test-user-service.sh   # user-service 测试
├── test-problem-service.sh # problem-service 测试
├── test-submission-service.sh # submission-service 测试
├── test-result-service.sh  # result-service 测试
├── test-e2e.sh            # 端到端测试
├── run-tests.sh           # 主入口脚本
└── README.md              # 本文档
```

---

## 完整测试流程

### 第一步：启动微服务

在运行测试之前，必须先启动所有微服务：

```bash
cd /home/xingyu/projects/mysql-db-practice/sql-judge-engine/docs/testing/scripts

# 启动所有服务（自动构建、启动、等待就绪）
./start-services.sh start

# 或者分步执行：
./start-services.sh build    # 仅构建
./start-services.sh start    # 启动服务
./start-services.sh status   # 查看状态
```

**启动脚本功能**：
- 检查 Docker 和 Docker Compose 环境
- 构建所有微服务镜像
- 启动基础设施（MySQL、RabbitMQ、Docker）
- 启动应用服务（user-service、problem-service 等）
- 等待服务就绪
- 初始化数据库和测试用户

### 第二步：检查服务健康状态

```bash
# 检查所有服务健康状态
./check-health.sh all

# 仅检查网络连接
./check-health.sh connectivity

# 仅检查 HTTP 响应
./check-health.sh http

# 检查基础设施（MySQL、RabbitMQ）
./check-health.sh infra

# 等待服务就绪（最多等待60秒）
./check-health.sh wait 60 5
```

**健康检查结果示例**：
```
Service                 Status                  Endpoint
------------------------------------------------------------
user-service            UP (auth)               http://localhost:8081/api/user/me
problem-service         UP                      http://localhost:8082/api/problem
submission-service      UP                      http://localhost:8083/api/submission
result-service          UP                      http://localhost:8086/api/result/leaderboard
container-manager       UP                      http://localhost:8085/health
judge-service           UP                      http://localhost:8084/health
```

### 第三步：运行黑盒测试

```bash
# 运行所有测试
./run-tests.sh all

# 运行特定服务测试
./run-tests.sh user        # user-service (8个测试)
./run-tests.sh problem     # problem-service (10个测试)
./run-tests.sh submission  # submission-service (8个测试)
./run-tests.sh result      # result-service (8个测试)
./run-tests.sh e2e        # 端到端测试 (5个测试)

# 交互式菜单
./run-tests.sh
```

---

## 服务启动脚本 (start-services.sh)

### 用法

```bash
./start-services.sh [command]

Commands:
    start       启动所有微服务（默认）
    stop        停止所有微服务
    restart     重启所有微服务
    build       仅构建服务
    logs        查看日志（使用: ./start-services.sh logs [service]）
    status      查看服务状态
    health      运行健康检查
    init        初始化数据库和测试数据
    help        显示帮助
```

### 示例

```bash
# 完整启动流程
./start-services.sh start

# 仅查看日志
./start-services.sh logs

# 查看特定服务日志
./start-services.sh logs user-service
./start-services.sh logs judge-service

# 停止所有服务
./start-services.sh stop

# 重启所有服务
./start-services.sh restart
```

### 启动的服务列表

| 服务 | 端口 | 说明 |
|------|------|------|
| Gateway | 8080 | API 网关 |
| user-service | 8081 | 用户管理服务 |
| problem-service | 8082 | 题目管理服务 |
| submission-service | 8083 | 提交服务 |
| result-service | 8086 | 结果服务 |
| container-manager | 8085 | 容器管理服务 |
| judge-service | 8084 | 判题服务 |
| MySQL | 3306 | 业务数据库 |
| RabbitMQ | 5672, 15672 | 消息队列 + 管理界面 |

---

## 健康检查脚本 (check-health.sh)

### 用法

```bash
./check-health.sh [check_type] [max_wait] [interval]

Check Types:
    connectivity   检查 TCP 连接
    http           检查 HTTP 端点响应
    infra          检查基础设施（MySQL、RabbitMQ、Docker）
    wait           等待服务就绪
    all            执行所有检查（默认）
```

### 示例

```bash
# 等待服务就绪（最多等待120秒）
./check-health.sh wait 120 5

# 检查所有
./check-health.sh all
```

### 健康检查端点

项目中有两个服务实现了 `/health` 端点：

| 服务 | Health 端点 | 说明 |
|------|-------------|------|
| container-manager | `/health` | 返回 `{"status":"UP","timestamp":"..."}` |
| judge-service | `/health` | 返回 `{"status":"UP","timestamp":"..."}` |

其他服务通过其主要 API 端点验证可用性。

---

## 测试用例列表

### user-service (8 个测试)

| 测试ID | 测试名称 | 验证内容 |
|--------|----------|----------|
| TC-USER-001 | User Registration Success | 用户注册成功返回 201 |
| TC-USER-002 | User Registration Duplicate | 用户名重复返回 409 |
| TC-USER-003 | User Login Success | 登录成功返回 JWT Token |
| TC-USER-004 | User Login Wrong Password | 密码错误返回 401 |
| TC-USER-005 | User Login Not Found | 用户不存在返回 401 |
| TC-USER-006 | Get Current User Info | 获取当前用户信息需认证 |
| TC-USER-007 | Get User by ID | 通过 ID 获取用户信息 |
| TC-USER-008 | Get User without Token | 无 Token 访问被拒绝 |

### problem-service (10 个测试)

| 测试ID | 测试名称 | 验证内容 |
|--------|----------|----------|
| TC-PROB-001 | Create Problem | 教师创建题目返回 201 |
| TC-PROB-002 | Get Problem List | 题目列表无需认证 |
| TC-PROB-003 | Get Problem List Filtered | 题目列表支持过滤 |
| TC-PROB-004 | Get Problem by ID | 通过 ID 获取题目详情 |
| TC-PROB-005 | Update Problem | 教师更新题目 |
| TC-PROB-006 | Update Problem Status | 更新题目状态（发布） |
| TC-PROB-007 | Delete Problem | 教师删除题目返回 204 |
| TC-PROB-008 | Get Teacher's Problems | 获取教师自己的题目 |
| TC-PROB-009 | Student Cannot Create | 学生不能创建题目 |
| TC-PROB-010 | Batch Import | 批量导入题目 |

### submission-service (8 个测试)

| 测试ID | 测试名称 | 验证内容 |
|--------|----------|----------|
| TC-SUB-001 | Submit Answer | 学生提交答案返回 202 |
| TC-SUB-002 | Get Submission Status | 获取提交状态 |
| TC-SUB-003 | Get Submission Detail | 获取提交详情 |
| TC-SUB-004 | Get Submission History | 获取学生提交历史 |
| TC-SUB-005 | Submit Invalid Problem | 提交到无效题目 |
| TC-SUB-006 | Submit without User ID | 无用户ID提交被拒绝 |
| TC-SUB-007 | Wait for Judging | 轮询等待判题完成 |
| TC-SUB-008 | Filter by Problem ID | 按题目ID过滤提交 |

### result-service (8 个测试)

| 测试ID | 测试名称 | 验证内容 |
|--------|----------|----------|
| TC-RESULT-001 | Get Result by Submission | 通过提交ID获取结果 |
| TC-RESULT-002 | Get Student Results | 获取学生成绩列表 |
| TC-RESULT-003 | Get Problem Leaderboard | 获取题目排行榜 |
| TC-RESULT-004 | Get Overall Leaderboard | 获取总排行榜 |
| TC-RESULT-005 | Get Result without Auth | 无认证获取结果被拒绝 |
| TC-RESULT-006 | Get Non-existent Result | 获取不存在的结果 |
| TC-RESULT-007 | Get Non-existent Leaderboard | 获取不存在的排行榜 |
| TC-RESULT-008 | Results Pagination | 结果分页 |

### 端到端测试 (5 个测试)

| 测试ID | 测试名称 | 验证内容 |
|--------|----------|----------|
| TC-E2E-001 | Complete Answer Flow | 教师创建→学生答题→查看成绩 |
| TC-E2E-002 | Error Handling Flow | 提交到无效题目→错误处理 |
| TC-E2E-003 | Resubmission Flow | 学生重复提交答案 |
| TC-E2E-004 | Concurrent Submissions | 多学生并发提交 |
| TC-E2E-005 | Teacher Workflow | 教师完整工作流程 |

---

## 配置

编辑 `config.sh` 文件，配置服务 URL 和测试用户：

```bash
# 服务地址
export USER_SERVICE_URL="http://localhost:8081"
export PROBLEM_SERVICE_URL="http://localhost:8082"
export SUBMISSION_SERVICE_URL="http://localhost:8083"
export RESULT_SERVICE_URL="http://localhost:8086"
export CONTAINER_MANAGER_URL="http://localhost:8085"
export JUDGE_SERVICE_URL="http://localhost:8084"

# 测试用户（需要在数据库中预先存在）
export EXISTING_TEACHER_USERNAME="teacher1"
export EXISTING_TEACHER_PASSWORD="password"
export EXISTING_STUDENT_USERNAME="student1"
export EXISTING_STUDENT_PASSWORD="password"
```

---

## 预期结果

### 成功输出示例

```
========================================
USER-SERVICE BLACK BOX TESTS
========================================

========================================
TEST: TC-USER-001: User Registration Success
========================================

[INFO] POST http://localhost:8081/api/user/register
[SUCCESS] Status: 201 (expected: 201)
[SUCCESS] Field 'id' = '123'
[SUCCESS] TEST PASSED: TC-USER-001

========================================
USER-SERVICE TEST SUMMARY
========================================
Passed: 8
Failed: 0

[SUCCESS] ALL USER-SERVICE TESTS PASSED!
```

### 失败输出示例

```
========================================
TEST: TC-USER-003: User Login Success
========================================

[INFO] POST http://localhost:8081/api/user/login
[ERROR] Status code mismatch
[ERROR] Expected: 200, Actual: 401
[ERROR] TEST FAILED: TC-USER-003
```

---

## 故障排查

### 服务不可连接

```bash
# 使用健康检查脚本诊断
./check-health.sh all

# 手动检查服务是否运行
docker-compose -f /home/xingyu/projects/mysql-db-practice/sql-judge-engine/docker-compose.yml ps

# 检查端口是否监听
netstat -tlnp | grep 8081
```

### 服务启动失败

```bash
# 查看服务日志
./start-services.sh logs

# 查看特定服务日志
./start-services.sh logs user-service
./start-services.sh logs mysql

# 重新构建
./start-services.sh build
```

### 测试用户不存在

```bash
# 初始化测试用户
./start-services.sh init

# 或手动创建用户
curl -X POST http://localhost:8081/api/user/register \
  -H "Content-Type: application/json" \
  -d '{"username":"teacher1","password":"password","role":"TEACHER","email":"teacher@test.com"}'
```

### RabbitMQ 消息未消费

端到端测试依赖于 RabbitMQ 和 judge-service。如果判题未完成：

```bash
# 检查 RabbitMQ 是否运行
docker-compose -f /home/xingyu/projects/mysql-db-practice/sql-judge-engine/docker-compose.yml ps rabbitmq

# 检查 RabbitMQ 管理界面
curl http://localhost:15672/api/healthchecks/node

# 检查 judge-service 日志
./start-services.sh logs judge-service
```

### 容器池无可用容器

```bash
# 检查 container-manager 状态
curl http://localhost:8085/api/pool/status

# 检查 Docker 是否运行
docker ps
```

---

## 完整测试工作流

```bash
# 1. 进入脚本目录
cd /home/xingyu/projects/mysql-db-practice/sql-judge-engine/docs/testing/scripts

# 2. 启动所有服务
./start-services.sh start

# 3. 检查服务健康状态
./check-health.sh all

# 4. 运行所有黑盒测试
./run-tests.sh all

# 5. 查看测试结果
# 测试日志保存在 test-results/ 目录

# 6. 停止所有服务（测试完成后）
./start-services.sh stop
```

---

## 扩展测试

### 添加新测试用例

1. 在对应的测试脚本中添加函数
2. 遵循命名规范：`test_<service>_<number>_<name>`
3. 在 `run_all_<service>_tests()` 函数中添加调用

### 自定义测试数据

修改 `config.sh` 中的配置：
- `TEST_TEACHER_USERNAME/PASSWORD`
- `TEST_STUDENT_USERNAME/PASSWORD`
- `EXISTING_TEACHER_USERNAME/PASSWORD`
- `EXISTING_STUDENT_USERNAME/PASSWORD`

---

## 参考文档

- [黑盒测试架构文档](../black-box-test-architecture.md)
- [微服务架构文档](../../architecture/microservices.md)
- [数据流文档](../../architecture/data-flow.md)
- [docker-compose.yml](../../docker-compose.yml)
