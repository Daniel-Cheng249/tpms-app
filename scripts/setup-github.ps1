# TPMS App GitHub 仓库初始化脚本
# 使用方法: .\scripts\setup-github.ps1 -Username "你的GitHub用户名"

param(
    [Parameter(Mandatory=$true)]
    [string]$Username,

    [string]$RepoName = "tpms-app",

    [switch]$Private,

    [switch]$UseSSH
)

function Write-ColorOutput($ForegroundColor) {
    $fc = $host.UI.RawUI.ForegroundColor
    $host.UI.RawUI.ForegroundColor = $ForegroundColor
    if ($args) {
        Write-Output $args
    }
    $host.UI.RawUI.ForegroundColor = $fc
}

Write-ColorOutput Cyan "=== TPMS App GitHub 仓库初始化 ==="
Write-Output ""

# 检查 git
if (-not (Get-Command git -ErrorAction SilentlyContinue)) {
    Write-ColorOutput Red "错误: 未安装 Git"
    exit 1
}

# 检查 gh CLI
$hasGh = Get-Command gh -ErrorAction SilentlyContinue

# 检查是否在项目根目录
if (-not (Test-Path "app\build.gradle.kts")) {
    Write-ColorOutput Red "错误: 请在项目根目录运行此脚本"
    exit 1
}

# 配置 Git 用户信息 (如果没有)
$userName = git config user.name
$userEmail = git config user.email

if (-not $userName) {
    $newName = Read-Host "请输入你的 Git 用户名"
    git config user.name "$newName"
}

if (-not $userEmail) {
    $newEmail = Read-Host "请输入你的 Git 邮箱"
    git config user.email "$newEmail"
}

Write-ColorOutput Green "Git 用户: $(git config user.name) <$(git config user.email)>"
Write-Output ""

# 检查是否已有远程仓库
$existingRemote = git remote get-url origin 2>$null
if ($existingRemote) {
    Write-ColorOutput Yellow "已有远程仓库: $existingRemote"
    $replace = Read-Host "是否替换? (y/N)"
    if ($replace -eq 'y' -or $replace -eq 'Y') {
        git remote remove origin
    } else {
        exit 0
    }
}

# 创建 GitHub 仓库
if ($hasGh) {
    Write-ColorOutput Cyan "使用 GitHub CLI 创建仓库..."

    $visibility = if ($Private) { "--private" } else { "--public" }

    gh repo create $RepoName $visibility --source=. --remote=origin --push

    if ($LASTEXITCODE -eq 0) {
        Write-ColorOutput Green "仓库创建并推送成功!"
        Write-Output ""
        Write-ColorOutput Cyan "仓库地址: https://github.com/$Username/$RepoName"
    } else {
        Write-ColorOutput Red "创建失败，请手动创建"
    }
} else {
    # 手动配置远程
    if ($UseSSH) {
        $remoteUrl = "git@github.com:$Username/$RepoName.git"
    } else {
        $remoteUrl = "https://github.com/$Username/$RepoName.git"
    }

    Write-ColorOutput Cyan "配置远程仓库: $remoteUrl"
    git remote add origin $remoteUrl

    Write-Output ""
    Write-ColorOutput Yellow "请先在 GitHub 上创建仓库: https://github.com/new"
    Write-ColorOutput Yellow "仓库名: $RepoName"
    Write-Output ""
    Write-ColorOutput Cyan "创建后运行以下命令推送:"
    Write-Output "  git branch -M main"
    Write-Output "  git push -u origin main"
}
