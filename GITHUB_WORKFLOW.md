# TPMS App GitHub 工作流指南

## 快速开始

### 1. 首次设置（二选一）

#### 方式 A: 使用 PowerShell 脚本（推荐）

```powershell
# 进入项目目录
cd D:\projects_cz\1_tpms\tpms-app

# 初始化 GitHub 仓库
.\scripts\setup-github.ps1 -Username "你的GitHub用户名"

# 按提示完成配置
```

#### 方式 B: 手动配置

```bash
# 配置 Git 用户信息（如果还没设置）
git config user.name "你的名字"
git config user.email "你的邮箱"

# 添加远程仓库
git remote add origin https://github.com/你的用户名/tpms-app.git

# 或者使用 SSH
git remote add origin git@github.com:你的用户名/tpms-app.git
```

### 2. 日常推送（三选一）

#### 方式 A: PowerShell 脚本（推荐）

```powershell
# 快速推送
.\scripts\git-push.ps1 -Message "修复了 BLE 连接问题"

# 强制推送（不询问确认）
.\scripts\git-push.ps1 -Message "更新 UI" -Force

# 推送到特定分支
.\scripts\git-push.ps1 -Message "新功能" -Branch "feature/ble-parser"
```

#### 方式 B: 命令行别名

添加到 PowerShell 配置文件 (`notepad $PROFILE`)：

```powershell
# TPMS 快捷推送
function tpms-push {
    param([string]$msg)
    if (-not $msg) { $msg = "更新代码" }
    D:\projects_cz\1_tpms\tpms-app\scripts\git-push.ps1 -Message $msg -Force
}

# 使用: tpms-push "修复 bug"
```

#### 方式 C: 传统 Git 命令

```bash
# 添加所有变更
git add .

# 提交
git commit -m "提交信息"

# 推送
git push origin main
```

### 3. Android Studio 集成

#### 设置 Git 推送快捷方式

1. **Settings → Version Control → Commit**
   - 勾选 "Sign-off commit"
   - 设置默认提交信息格式

2. **工具栏快捷按钮**
   - 右键工具栏 → **Customize Toolbar**
   - 添加 **Push** 按钮到常用位置

3. **快捷键设置 (Settings → Keymap)**
   - `Ctrl+K` - 提交 (Commit)
   - `Ctrl+Shift+K` - 推送 (Push)

4. **提交前自动格式化代码**
   - Settings → Tools → Actions on Save
   - 勾选 "Reformat code" 和 "Optimize imports"

## 分支策略

### 推荐的工作流程

```
main        ●────●────●────●────●  (生产环境，稳定)
             ╱    ╱    ╱
develop  ───●────●────●────●────●  (开发分支，日常推送)
                ╱    ╱
feature/ble ───●────●               (功能分支)
```

### 常用分支命令

```bash
# 创建并切换到新分支
git checkout -b feature/smp290-parser

# 推送新分支到远程
git push -u origin feature/smp290-parser

# 合并到 main
git checkout main
git merge feature/smp290-parser
git push origin main
```

## GitHub Actions 自动构建

本项目已配置 CI/CD 工作流：

| 触发条件 | 执行操作 |
|---------|---------|
| Push 到 main/develop | 自动构建 + 单元测试 |
| Pull Request | 构建检查 |
| 推送标签 v* | 自动发布 APK |

### 查看构建状态

访问: `https://github.com/你的用户名/tpms-app/actions`

## 提交信息规范

### 格式

```
<类型>: <简短描述>

<详细描述（可选）>

<相关 Issue（可选）>
```

### 类型说明

| 类型 | 用途 |
|-----|-----|
| `feat` | 新功能 |
| `fix` | 修复 bug |
| `docs` | 文档更新 |
| `style` | 代码格式调整 |
| `refactor` | 重构 |
| `test` | 测试相关 |
| `chore` | 构建/工具相关 |

### 示例

```
feat: 实现 SMP290 数据解析器

- 添加字节序转换
- 处理校验和验证
- 添加错误日志

Closes #12
```

## 常见问题

### Q: 推送被拒绝 "rejected: non-fast-forward"

```bash
# 先拉取远程更新
git pull origin main --rebase

# 然后再推送
git push origin main
```

### Q: 忘记添加文件到 .gitignore

```bash
# 从 Git 移除但保留本地文件
git rm --cached local.properties

# 提交 .gitignore 更新
git add .gitignore
git commit -m "chore: 更新忽略文件"
```

### Q: 想撤销上一次提交

```bash
# 保留修改，仅撤销提交
git reset --soft HEAD~1

# 完全丢弃修改（危险！）
git reset --hard HEAD~1
```

## 推荐工具

| 工具 | 用途 |
|-----|-----|
| GitHub Desktop | 图形化 Git 操作 |
| GitKraken | 高级分支可视化 |
| GitLens (VS Code) | 代码历史追溯 |
| tig | 命令行交互式浏览 |

## 配置 SSH（免密码推送）

```bash
# 生成 SSH 密钥
ssh-keygen -t ed25519 -C "你的邮箱"

# 添加到 ssh-agent
ssh-add ~/.ssh/id_ed25519

# 复制公钥到 GitHub
cat ~/.ssh/id_ed25519.pub | clip
# 然后在 GitHub Settings → SSH Keys 中添加

# 修改远程为 SSH
git remote set-url origin git@github.com:用户名/tpms-app.git
```
