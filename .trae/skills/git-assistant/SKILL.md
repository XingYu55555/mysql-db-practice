---
name: "git-assistant"
description: "Git workflow assistant for commit, review, summary, and merge operations. Invoke when user needs help with git commands, code review before commit, generating commit messages, or repository management."
---

# Git Assistant

This skill provides comprehensive Git workflow assistance including code review before commit, generating meaningful commit messages, repository status checks, and merge operations.

## When to Invoke

- User asks to commit code
- User wants to review changes before commit
- User needs help with git commands
- User wants to generate commit message
- User needs to merge branches
- User wants to check repository status
- User needs to push/pull code

## Capabilities

### 1. Pre-commit Code Review
Before committing, automatically:
- Check git status to see what files changed
- Review the diff for common issues
- Suggest improvements
- Check for sensitive data (passwords, API keys)
- Verify code style consistency

### 2. Commit Message Generation
Generate conventional commit messages:
```
<type>(<scope>): <subject>

<body>

<footer>
```

Types:
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes (formatting, semicolons, etc.)
- `refactor`: Code refactoring
- `test`: Adding or updating tests
- `chore`: Build process or auxiliary tool changes

### 3. Repository Status Summary
Provide clear summary of:
- Current branch
- Modified files
- Staged files
- Untracked files
- Ahead/behind remote status

### 4. Merge Assistance
Help with:
- Checking merge conflicts
- Resolving conflicts
- Creating merge commits
- Squash merges

## Workflow

### Standard Commit Workflow

1. **Check Status**
   ```bash
   git status
   git diff --stat
   ```

2. **Review Changes**
   - Show diff for each modified file
   - Check for issues
   - Suggest improvements

3. **Stage Files**
   ```bash
   git add <files>
   ```

4. **Generate Commit Message**
   - Analyze changes
   - Generate conventional commit message
   - Allow user to edit

5. **Commit**
   ```bash
   git commit -m "<message>"
   ```

6. **Push (if requested)**
   ```bash
   git push origin <branch>
   ```

### Pre-commit Checklist

- [ ] No sensitive data in code
- [ ] No debug console.log statements
- [ ] Code follows project style
- [ ] Tests pass (if applicable)
- [ ] Commit message is descriptive

## Commands Reference

### Status Commands
```bash
git status                    # Check repository status
git branch -v                 # Show branches with last commit
git log --oneline -10         # Show recent commits
git diff                      # Show unstaged changes
git diff --cached             # Show staged changes
```

### Commit Commands
```bash
git add <file>                # Stage specific file
git add .                     # Stage all changes
git commit -m "message"       # Commit with message
git commit --amend            # Amend last commit
```

### Branch Commands
```bash
git branch <name>             # Create branch
git checkout <branch>         # Switch branch
git checkout -b <name>        # Create and switch
git merge <branch>            # Merge branch
git branch -d <name>          # Delete branch
```

### Remote Commands
```bash
git remote -v                 # Show remotes
git fetch origin              # Fetch changes
git pull origin <branch>      # Pull changes
git push origin <branch>      # Push changes
git push -u origin <branch>   # Push and set upstream
```

## Best Practices

1. **Commit Often**: Make small, focused commits
2. **Write Good Messages**: Clear, descriptive commit messages
3. **Review Before Commit**: Always review changes before committing
4. **Keep Main Clean**: Don't commit broken code to main branch
5. **Use Branches**: Create feature branches for new work
6. **Pull Before Push**: Always pull latest changes before pushing

## Example Usage

User: "帮我提交代码"
Assistant:
1. Run `git status` to see changes
2. Run `git diff` to review changes
3. Check for issues
4. Stage files with `git add`
5. Generate commit message
6. Commit with `git commit`
7. Push with `git push` (if requested)
