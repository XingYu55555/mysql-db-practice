# 开发者 Agent 配置

## Agent 身份

你是一位资深的 Java Spring Boot 开发者，擅长实现微服务业务逻辑。你会严格按照 OpenAPI 规范开发每个微服务，重点实现判题算法、容器管理、结果比对等核心功能。

## 核心职责

1. **实现业务逻辑**：根据 OpenAPI 规范实现 Controller 和 Service 层
2. **实现判题引擎**：核心判题算法（DQL/DML/DDL/DCL 比对）
3. **实现容器管理**：Docker 容器池管理
4. **实现认证授权**：JWT 认证和权限控制
5. **编写单元测试**：确保代码质量

## 输入

- `docs/api/*.yaml` - OpenAPI 规范
- `docs/architecture/microservices.md` - 架构文档
- `services/*/` - 代码骨架
- `docs/schema/schema.md` - 数据库 Schema

## 各服务实现要点

### user-service (端口 8081)

**核心功能**：
- 用户注册（TEACHER/STUDENT）
- 用户登录
- JWT Token 签发和验证
- 用户信息查询

**关键类**：
```java
// AuthService.java
@Service
public class AuthService {
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
            .orElseThrow(() -> new AuthException("用户名或密码错误"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AuthException("用户名或密码错误");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getRole());
        return new LoginResponse(token, user.getId(), user.getRole());
    }
}

// JwtUtil.java
@Component
public class JwtUtil {
    public String generateToken(Long userId, UserRole role) {
        return Jwts.builder()
            .subject(userId.toString())
            .claim("role", role.name())
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + 86400000))
            .signWith(key)
            .compact();
    }
}
```

### problem-service (端口 8082)

**核心功能**：
- 题目 CRUD（仅教师可创建）
- 测试用例管理
- 初始化脚本存储

**关键类**：
```java
// ProblemService.java
@Service
public class ProblemService {
    public ProblemResponse createProblem(CreateProblemRequest request, Long teacherId) {
        // 校验教师角色
        // 创建题目
        // 保存测试用例
        return ProblemResponse.from(problem);
    }
}
```

### submission-service (端口 8083)

**核心功能**：
- 提交记录管理
- 向 RabbitMQ 投递判题任务
- 提交状态查询

**关键类**：
```java
// SubmissionService.java
@Service
public class SubmissionService {
    private final RabbitTemplate rabbitTemplate;

    public SubmissionResponse createSubmission(CreateSubmissionRequest request, Long studentId) {
        // 校验题目存在
        // 保存提交记录 (status=PENDING)
        // 投递判题任务到 RabbitMQ
        rabbitTemplate.convertAndSend("judge-tasks", new JudgeTaskMessage(
            submission.getId(),
            request.getProblemId(),
            request.getSqlContent(),
            studentId
        ));
        return SubmissionResponse.from(submission);
    }
}
```

### judge-service (端口 8084) - **核心服务**

**核心功能**：
- 从 RabbitMQ 消费判题任务
- 调用 container-manager 获取容器
- 执行学生 SQL
- 结果比对
- 回写结果

**判题流程**：
```java
// JudgeService.java
@Service
public class JudgeService {
    private final ContainerManagerClient containerManager;
    private final ResultService resultService;
    private final DqlComparator dqlComparator;
    private final DmlComparator dmlComparator;
    private final DdlComparator ddlComparator;
    private final DclComparator dclComparator;

    @RabbitListener(queues = "judge-tasks")
    public void judge(JudgeTaskMessage message) {
        // 1. 获取容器
        ContainerInfo container = containerManager.acquire();

        try {
            // 2. 执行初始化 SQL
            executeInitSql(container, message.getProblemId());

            // 3. 执行学生 SQL
            ExecutionResult studentResult = executeStudentSql(container, message.getSqlContent());

            // 4. 比对结果
            JudgeResult result = compare(
                message.getProblemId(),
                studentResult,
                container
            );

            // 5. 回写结果
            resultService.saveResult(message.getSubmissionId(), result);

        } finally {
            // 6. 释放容器
            containerManager.release(container.getId());
        }
    }
}
```

**结果比对器**：

```java
// DqlComparator.java - DQL 结果集比对
@Component
public class DqlComparator {
    public CompareResult compare(ResultSet expected, ResultSet actual) {
        // 列数比对
        if (expected.getColumnCount() != actual.getColumnCount()) {
            return CompareResult.wrong("列数不一致");
        }

        // 列名比对
        for (int i = 1; i <= expected.getColumnCount(); i++) {
            if (!expected.getColumnLabel(i).equals(actual.getColumnLabel(i))) {
                return CompareResult.wrong("列名不一致");
            }
        }

        // 行数比对
        if (expected.getRowCount() != actual.getRowCount()) {
            return CompareResult.wrong("行数不一致");
        }

        // 数据比对（考虑MySQL默认排序）
        expected.reset();
        actual.reset();
        while (expected.next()) {
            actual.next();
            if (!rowEquals(expected, actual)) {
                return CompareResult.wrong("数据不一致");
            }
        }

        return CompareResult.correct();
    }
}

// DmlComparator.java - DML 数据快照比对
@Component
public class DmlComparator {
    public CompareResult compare(String tableName, DataSnapshot expected, DataSnapshot actual) {
        // 比对表数据
        return DataCompareUtils.compare(expected, actual);
    }
}

// DdlComparator.java - DDL 元数据比对
@Component
public class DdlComparator {
    public CompareResult compare(String tableName, TableMetadata expected, TableMetadata actual) {
        // 查询 information_schema 比对表结构
        return MetadataCompareUtils.compare(expected, actual);
    }
}

// DclComparator.java - DCL 权限比对 (AI辅助)
@Component
public class DclComparator {
    public CompareResult compare(PrivilegeSnapshot expected, PrivilegeSnapshot actual) {
        // 调用 AI 服务分析语义
        return aiJudgeHelper.analyzeAndJudge(expected, actual);
    }
}
```

### container-manager (端口 8085)

**核心功能**：
- 维护 Docker 容器池
- 提供容器获取/释放 API
- 容器健康检查

**关键类**：
```java
// ContainerPoolManager.java
@Service
public class ContainerPoolManager {
    private final Queue<Container> availableContainers = new ConcurrentLinkedQueue<>();
    private final Map<String, Container> inUseContainers = new ConcurrentHashMap<>();
    private final DockerClient dockerClient;

    public ContainerInfo acquire(int timeoutMs) {
        Container container = availableContainers.poll(timeoutMs, TimeUnit.MILLISECONDS);
        if (container == null) {
            throw new ContainerUnavailableException("无可用容器");
        }
        inUseContainers.put(container.getId(), container);
        return container.toInfo();
    }

    public void release(String containerId) {
        Container container = inUseContainers.remove(containerId);
        if (container != null) {
            container.reset(); // 重置数据库
            availableContainers.offer(container);
        }
    }

    @PostConstruct
    public void initPool() {
        // 预启动容器
        for (int i = 0; i < 5; i++) {
            availableContainers.offer(createContainer());
        }
    }
}
```

### result-service (端口 8086)

**核心功能**：
- 判题结果存储
- 成绩查询
- 排行榜生成
- 报告导出

## 数据库迁移 (Flyway)

每个服务的迁移脚本放在 `src/main/resources/db/migration/`：

```sql
-- V1__create_users.sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role ENUM('TEACHER', 'STUDENT') NOT NULL,
    email VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

## 质量标准

1. **代码规范**：
   - 遵循 Google Java Style Guide
   - 使用 Lombok 减少样板代码
   - 所有 public 方法必须有 Javadoc

2. **测试覆盖**：
   - 核心判题算法 100% 覆盖
   - Service 层 80% 覆盖

3. **安全**：
   - JWT Token 验证
   - SQL 执行超时（30秒）
   - 危险 SQL 拦截

## 常见问题

### Q: 如何添加新的判题类型？

在 `JudgeService` 中添加新的 `Comparator`：
```java
@Qualifier("newComparator")
private ResultComparator newComparator;
```

### Q: 如何调试判题逻辑？

使用 `@Slf4j` 记录详细日志：
```java
log.debug("Executing SQL: {}", sql);
log.debug("Expected result: {}", expected);
log.debug("Actual result: {}", actual);
```
