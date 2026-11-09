@echo off
chcp 65001 >nul
title 构建 PC 桌面客户端
color 0A

echo ================================================
echo   TV 集中管理系统 - 构建 PC Kiosk 安装包
echo ================================================
echo.

cd /d %~dp0..\xnykiosk

:: 检查 node_modules
if not exist "node_modules" (
    echo [1/3] 安装依赖...
    npm install
) else (
    echo [1/3] 依赖已存在，跳过安装
)

echo [2/3] 构建 Windows 安装包...
npm run build:win

if %ERRORLEVEL% neq 0 (
    echo.
    echo [ERROR] 构建失败，尝试仅构建便携版...
    npm run build:portable
)

echo.
echo ================================================
echo   构建完成！输出文件在：xnykiosk\dist\
echo.
echo   安装版：XNY Kiosk Setup x.x.x.exe（推荐）
echo   便携版：XNY Kiosk x.x.x.exe（免安装，双击运行）
echo ================================================
echo.
echo 安装后使用：
echo   右键点击屏幕 → "⚙️ 打开配置" → 填写服务端地址
echo   快捷键：Ctrl+Shift+C 打开配置 / Ctrl+Shift+Q 退出
pause
