---
name: "document-driven-development"
description: "Ensures all code changes align with project documentation. Invoke before making any code modifications to verify documentation exists and follow it strictly."
---

# Document-Driven Development

This skill ensures that all code changes are based on and consistent with existing project documentation.

## When to Invoke

**MANDATORY: You MUST invoke this skill as the FIRST step before:**
- Modifying any source code files
- Changing configuration files
- Adding new dependencies
- Modifying docker-compose or deployment configurations
- Creating new services or APIs
- Any code editing operation

**Trigger phrase**: "文档驱动" or when making code changes

## Core Principle

> **"Never guess, always verify against documentation"**

## Workflow

### Step 1: Find Relevant Documentation
Before making any changes, locate and read the relevant documentation:
- Architecture documents (`docs/architecture/*.md`)
- API specifications (`docs/api/*.yaml`)
- Database schemas (`docs/schema/*.sql`)
- README files
- Configuration files

### Step 2: Verify Design Intent
Confirm that:
- The proposed change aligns with documented design
- No documented constraints are violated
- Service responsibilities are respected
- API boundaries are maintained

### Step 3: If Documentation Conflicts with Implementation
When code doesn't match documentation:
1. **Do NOT** modify code to match undocumented behavior
2. Report the discrepancy to the user
3. Ask whether to:
   - Update documentation first, then implement
   - Fix implementation to match documentation

### Step 4: Reference During Changes
When making changes, explicitly cite documentation:
- "根据 architecture.md 第 X 节，..."
- "依据 API 规范，..."
- "符合文档中定义的设计"

## Common Documentation Locations

| Document Type | Location |
|---------------|----------|
| Architecture | `docs/architecture/microservices.md` |
| Data Flow | `docs/architecture/data-flow.md` |
| API Specs | `docs/api/` |
| Database Schema | `configs/mysql/init.sql` |
| Service READMEs | `services/*/README.md` |

## Red Flags (Don't Do These)

❌ **Don't assume** - "I think it should work this way..."
❌ **Don't guess** - "Maybe we can just..."
❌ **Don't guess** - "The design probably means..."
❌ **Don't improvise** - "Let's try this approach..."

✅ **Do verify** - "According to section X of architecture.md..."
✅ **Do reference** - "The documentation specifies..."
✅ **Do confirm** - "Is this consistent with your intent?"

## Example Workflow

**User Request**: "Fix the health check issue for judge-service"

**Wrong Approach**:
```
Adding a healthcheck to judge-service since it's not starting
```

**Correct Approach**:
```
1. Read microservices.md - Section 2.1 shows judge-service has NO HTTP API
2. Read microservices.md - Section 6.4 shows judge-service is "internal service"
3. Therefore: judge-service should NOT have a healthcheck endpoint
4. The issue is NOT the healthcheck but the depends_on configuration
5. According to Section 4.1: judge-service only needs MySQL and RabbitMQ at startup
```

## Output Format

When invoking this skill, state your verification:

```
根据 [文档名称] 第 [章节] 节：
- [文档中定义的设计/行为]
- [当前代码实现]
- [是否一致 / 需要修改的内容]
```
