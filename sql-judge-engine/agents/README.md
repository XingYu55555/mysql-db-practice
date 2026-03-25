# Agents 目录说明

本目录包含 Agent 的**详细提示词模板**，供 AI 助手在执行任务时参考使用。

## 目录结构

```
agents/
├── architect/          # 架构师 Agent 提示词
│   └── prompt.md
├── splitter/           # 分包 Agent 提示词
│   └── prompt.md
├── developer/          # 开发者 Agent 提示词
│   └── prompt.md
└── tester/             # 测试 Agent 提示词
    └── prompt.md
```

## 与 `.claude/agents/` 的关系

| 目录 | 格式 | 用途 |
|------|------|------|
| **`.claude/agents/`** | YAML | Claude 等 AI 工具自动发现的**结构化 Agent 定义** |
| **`agents/`** | Markdown | **详细提示词模板**，供 AI 助手在执行具体任务时参考 |

## 工作流程

```
当 Claude 等支持 .claude/ 目录的 AI 工具启动时：

1. AI 工具扫描 .claude/agents/*.yaml
   → 获取 Agent 的名称、描述、职责、能力

2. AI 工具在执行任务时，读取对应的 agents/*/prompt.md
   → 获取详细的执行指导、代码示例、最佳实践

3. AI 工具根据 prompt.md 中的指导执行任务
   → 输出代码、文档、测试等
```

## Agent 职责

| Agent | 职责 | 协调关系 |
|-------|------|----------|
| **architect** | 划分微服务边界、设计数据流、产出架构文档 | 为其他 Agent 提供架构指导 |
| **splitter** | 根据 OpenAPI 规范生成微服务代码骨架 | 读取 architect 产出，生成代码结构 |
| **developer** | 实现业务逻辑、判题算法 | 读取 splitter 产出，填充业务逻辑 |
| **tester** | 编写单元/集成/契约/E2E 测试 | 与 developer 协调，验证开发产出 |

## 协作流程

```
architect → splitter → developer ↔ tester
     ↓           ↓          ↓
  架构设计    代码骨架    测试覆盖
```

### 详细协作步骤

1. **architect** 产出架构文档和 OpenAPI 规范
2. **splitter** 根据 OpenAPI 生成代码骨架
3. **developer** 实现业务逻辑
4. **developer** 通知 **tester**："{service} 的 {module} 已完成"
5. **tester** 编写对应测试并反馈覆盖率
6. **developer** 根据反馈补充代码
7. 循环直到所有服务完成

## 使用方式

AI 助手（如 Claude）在执行任务时，可以这样使用这些提示词：

```
我需要你扮演 {Agent名称} Agent
请阅读 agents/{agent}/prompt.md 获取详细的执行指导
然后按照指导完成以下任务：{具体任务描述}
```
