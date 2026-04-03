# TPMS App Git 推送脚本
# 使用方法: .\scripts\git-push.ps1 -Message "你的提交信息"

param(
    [Parameter(Mandatory=$true)]
    [string]$Message,

    [string]$Branch = "main",

    [switch]$Force,

    [switch]$CreatePR
)

# 颜色输出
function Write-ColorOutput($ForegroundColor) {
    $fc = $host.UI.RawUI.ForegroundColor
    $host.UI.RawUI.ForegroundColor = $ForegroundColor
    if ($args) {
        Write-Output $args
    }
    $host.UI.RawUI.ForegroundColor = $fc
}

Write-ColorOutput Cyan "=== TPMS App Git 推送脚本 ==="
Write-Output ""

# 检查是否在正确的目录
if (-not (Test-Path "app\build.gradle.kts")) {
    Write-ColorOutput Red "错误: 请在项目根目录运行此脚本"
    exit 1
}

# 检查远程仓库
$remote = git remote get-url origin 2>$null
if (-not $remote) {
    Write-ColorOutput Yellow "未配置远程仓库，请先运行设置命令:"
    Write-Output "  git remote add origin https://github.com/你的用户名/tpms-app.git"
    exit 1
}

Write-ColorOutput Green "远程仓库: $remote"
Write-Output ""

# 检查是否有变更
$status = git status --porcelain
if (-not $status) {
    Write-ColorOutput Yellow "没有要提交的变更"
    exit 0
}

# 显示变更摘要
Write-ColorOutput Cyan "变更摘要:"
git status --short
Write-Output ""

# 确认推送
if (-not $Force) {
    $confirm = Read-Host "确认推送? (y/N)"
    if ($confirm -ne 'y' -and $confirm -ne 'Y') {
        Write-ColorOutput Yellow "已取消"
        exit 0
    }
}

# 添加所有变更
Write-ColorOutput Cyan "添加变更..."
git add .

# 提交
Write-ColorOutput Cyan "提交: $Message"
git commit -m "$Message"

if ($LASTEXITCODE -ne 0) {
    Write-ColorOutput Red "提交失败"
    exit 1
}

# 推送
Write-ColorOutput Cyan "推送到 $Branch..."
git push origin $Branch

if ($LASTEXITCODE -ne 0) {
    Write-ColorOutput Red "推送失败"
    exit 1
}

Write-ColorOutput Green "推送成功!"

# 如果需要创建 PR
if ($CreatePR) {
    Write-ColorOutput Cyan "创建 Pull Request..."
    gh pr create --title "$Message" --body "自动生成 PR"
}

Write-Output ""
Write-ColorOutput Green "完成!"
