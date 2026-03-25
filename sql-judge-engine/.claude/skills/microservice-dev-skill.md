# 微服务开发 Skill

## 概述

本 Skill 定义了 SQL 判题引擎微服务开发的完整流程，包括开发规范、技术选型、代码结构、质量标准等。

## 适用场景

- 从零开始开发新的微服务
- 现有微服务的维护和扩展
- 新团队成员快速上手

## 技术栈

| 组件 | 技术选型 | 版本 |
|------|----------|------|
| 后端框架 | Spring Boot | 3.2.x |
| 微服务框架 | Spring Cloud | 2023.x |
| 数据库 | MySQL | 8.0 |
| ORM | Spring Data JPA | - |
| 消息队列 | RabbitMQ | 3.x |
| 容器管理 | Docker SDK | - |
| API 文档 | OpenAPI 3.0 / Swagger | - |
| 代码生成 | openapi-generator | 7.x |
| 数据库迁移 | Flyway | - |
| 测试 | JUnit 5, Mockito, Testcontainers | - |
| 构建工具 | Maven | 3.9.x |

## 项目结构

```
sql-judge-engine/
├── services/
│   ├── user-service/
│   ├── problem-service/
│   ├── submission-service/
│   ├── judge-service/
│   ├── container-manager/
│   └── result-service/
├── docs/
│   ├── api/          # OpenAPI 规范
│   ├── architecture/ # 架构文档
│   └── schema/       # 数据库 Schema
├── shared/           # 共享库
└── test/            # 全局测试
```

## 服务开发规范

### 1. 命名规范

- **包名**: `com.sqljudge.{service-name}`
- **类名**: 大驼峰，如 `UserService`, `ProblemController`
- **方法名**: 小驼峰，如 `createUser`, `findById`
- **数据库表名**: 下划线命名，如 `user_problems`
- **API 路径**: RESTful，如 `/api/user/{id}`

### 2. Controller 层

```java
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(request));
    }
}
```

### 3. Service 层

```java
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserResponse create(CreateUserRequest request) {
        // 1. 业务校验
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("USERNAME_EXISTS");
        }

        // 2. 实体构建
        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .build();

        // 3. 保存
        user = userRepository.save(user);

        // 4. 返回
        return UserResponse.from(user);
    }
}
```

### 4. Repository 层

```java
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    Page<User> findByRole(UserRole role, Pageable pageable);
}
```

### 5. DTO 设计

- **Request DTO**: 以 `Create`/`Update`/`Query` 结尾
- **Response DTO**: 以 `Response` 结尾
- 使用 `@Valid` 进行参数校验
- 使用 `@NotNull`, `@NotBlank`, `@Size` 等注解

### 6. 异常处理

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(e.getCode(), e.getMessage()));
    }
}
```

### 7. 配置管理

使用 `application.yml` 管理配置：

```yaml
server:
  port: 8081

spring:
  datasource:
    url: jdbc:mysql://${DB_HOST}:3306/business_db
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false

jwt:
  secret: ${JWT_SECRET}
  expiration: ${JWT_EXPIRATION:86400000}

rabbitmq:
  host: ${RABBITMQ_HOST}
  port: ${RABBITMQ_PORT:5672}
```

## 判题服务核心实现

### 判题流程

```
1. 从 RabbitMQ 消费判题任务
2. 向 container-manager 申请容器
3. 连接容器内 MySQL
4. 创建题目数据库
5. 执行初始化 SQL
6. 执行学生 SQL
7. 比对结果
8. 回写结果到 result-service
9. 释放容器
```

### 结果比对策略

| SQL 类型 | 比对方式 | 关键点 |
|----------|----------|--------|
| DQL | 结果集比对 | 列数、列名、行数、数据、顺序（ORDER BY） |
| DML | 数据快照比对 | 执行前后数据差异 |
| DDL | 元数据比对 | information_schema |
| DCL | 权限元数据比对 | AI 辅助分析语义 |

### DQL 结果集比对

```java
public class DqlComparator {

    public CompareResult compare(ResultSet expected, ResultSet actual) {
        // 1. 比对列数
        if (expected.getColumnCount() != actual.getColumnCount()) {
            return CompareResult.wrong("列数不一致");
        }

        // 2. 比对列名
        for (int i = 1; i <= expected.getColumnCount(); i++) {
            if (!expected.getColumnLabel(i).equals(actual.getColumnLabel(i))) {
                return CompareResult.wrong("列名不一致: " + expected.getColumnLabel(i));
            }
        }

        // 3. 比对行数
        if (expected.getRowCount() != actual.getRowCount()) {
            return CompareResult.wrong("行数不一致");
        }

        // 4. 比对数据
        // 考虑 MySQL 默认排序，允许无 ORDER BY 时结果顺序不同
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
```

## 容器管理

### 容器池

```java
@Service
public class ContainerPoolManager {

    private final Queue<Container> availableContainers = new ConcurrentLinkedQueue<>();
    private final Map<String, Container> inUseContainers = new ConcurrentHashMap<>();

    public Container acquire(int timeoutMs) {
        Container container = availableContainers.poll(timeout, TimeUnit.MILLISECONDS);
        if (container == null) {
            throw new ContainerUnavailableException("无可用容器");
        }
        inUseContainers.put(container.getId(), container);
        return container;
    }

    public void release(String containerId) {
        Container container = inUseContainers.remove(containerId);
        if (container != null) {
            container.reset();
            availableContainers.offer(container);
        }
    }
}
```

## 质量标准

### 测试覆盖率

| 层级 | 最低覆盖率 |
|------|------------|
| 整体 | 80% |
| 核心判题算法 | 100% |
| Controller | 70% |
| Service | 80% |

### 代码规范

- 所有 public 方法必须有 Javadoc
- 遵循 Google Java Style Guide
- 使用 Lombok 减少样板代码
- 避免硬编码，使用配置

### 安全要求

- 所有 API 必须认证（除 /api/user/register, /api/user/login）
- JWT Token 有效期 24 小时
- 密码必须 BCrypt 加密
- SQL 执行设置超时（30秒）
- 危险 SQL（DROP DATABASE）必须拦截

## 文档要求

每个服务必须包含：

1. `README.md`: 服务说明、API 列表、本地开发指南
2. `openapi.yaml`: API 契约副本
3. `Dockerfile`: 容器化配置
4. `pom.xml`: Maven 依赖配置

## 常见问题

### Q: 如何添加新服务？

1. 在 `docs/api/` 创建 OpenAPI 规范
2. 使用 Splitter Agent 生成代码骨架
3. 使用 Developer Agent 实现业务逻辑
4. 使用 Tester Agent 编写测试

### Q: 如何调试容器内 MySQL？

```bash
docker exec -it <container_id> mysql -uroot -p<password>
```

### Q: 如何查看 RabbitMQ 消息？

访问 http://localhost:15672 (guest/guest)
