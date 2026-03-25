---
name: "context7-library-research"
description: "Use Context7 MCP server to query latest library documentation. Invoke when needing library usage examples, API references, or dependency configuration for Maven/Gradle/Java libraries."
---

# Context7 Library Research

This skill uses the Context7 MCP server to fetch up-to-date library documentation and usage examples.

## When to Invoke

**MANDATORY: Invoke this skill when:**
- Searching for library usage examples (Maven dependencies, API calls)
- Building system dependencies (pom.xml, build.gradle)
- Troubleshooting library compatibility issues
- Need latest library version information
- Missing class imports or API changes

## Tools Available

| Tool | Purpose |
|------|---------|
| `mcp_context7_resolve-library-id` | Resolve library name to Context7 ID |
| `mcp_context7_query-docs` | Query library documentation |

## Workflow

### Step 1: Resolve Library ID

When you have a library name but not the Context7 ID:

```
Input: "docker-java"
Output: "/docker-java/docker-java"
```

### Step 2: Query Documentation

When you need usage examples or API details:

```
Input:
- libraryId: "/docker-java/docker-java"
- query: "How to use Unix socket connection with docker-java"
```

## Common Library IDs

| Library | Context7 ID |
|---------|-------------|
| docker-java | `/docker-java/docker-java` |
| Spring Boot | `/spring-projects/spring-boot` |
| Maven | `/apache/maven` |
| Jackson | `/FasterXML/jackson` |

## Example Usage

### Finding Maven Dependency

```
1. Resolve library: docker-java
2. Query: "maven dependency artifactId version for Java 17"
```

### Troubleshooting Class Not Found

```
1. Resolve library: docker-java
2. Query: "ApacheDockerHttpClient class location package"
```

### Finding Correct Transport

```
1. Resolve library: docker-java
2. Query: "docker-java-transport-httpclient5 vs unixsocket which to use"
```

## Output Format

When using Context7, structure response as:

```
根据 Context7 最新文档 [library_name]：
- 正确依赖配置：<dependency snippet>
- 关键类/方法：<class or method>
- 使用示例：<code example>
```

## Error Handling

If Context7 returns no results:
1. Try alternative library name variations
2. Search web for official documentation
3. Report to user about library not found in Context7

## Integration with document-driven-development

This skill complements `document-driven-development`:
- `document-driven-development`: Verifies implementation matches project docs
- `context7-library-research`: Provides correct library usage when modifying code
