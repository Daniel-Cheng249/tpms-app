@echo off
chcp 65001 >nul
cd /d "%~dp0"

if "%~1"=="" (
    echo 用法: push.bat "提交信息"
    echo 示例: push.bat "修复了BLE连接bug"
    exit /b 1
)

echo 正在推送...
git add .
git commit -m "%~1"
git push origin master

echo.
echo 完成!
pause
