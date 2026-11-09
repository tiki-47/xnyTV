@echo off
chcp 65001 >nul
title 一键全量构建
color 0A

echo ╔═══════════════════════════════════════════╗
echo ║    TV 集中管理系统 - 一键全量构建         ║
echo ╚═══════════════════════════════════════════╝
echo.
echo 将依次构建：
echo   [1] 服务端 JAR（tvservice）
echo   [2] 安卓 APK（tvclient）
echo   [3] PC 客户端安装包（xnykiosk）
echo.
pause

:: ====== 构建服务端 ======
echo.
echo ─────────────────────────────────────────────
echo [1/3] 构建服务端 JAR...
echo ─────────────────────────────────────────────
set JAVA_HOME=C:\Program Files\Java\jdk-17
set PATH=%JAVA_HOME%\bin;%PATH%

cd /d %~dp0..\tvservice
call mvn clean package -DskipTests -q
if %ERRORLEVEL% neq 0 (
    echo [ERROR] 服务端构建失败！
    pause
    exit /b 1
)
echo [OK] 服务端构建完成

:: ====== 构建安卓 APK ======
echo.
echo ─────────────────────────────────────────────
echo [2/3] 构建安卓 APK...
echo ─────────────────────────────────────────────
cd /d %~dp0..\tvclient
call gradlew.bat assembleDebug --no-daemon -q
if %ERRORLEVEL% neq 0 (
    echo [WARN] APK 构建失败，跳过
) else (
    echo [OK] APK 构建完成
)

:: ====== 构建 PC 客户端 ======
echo.
echo ─────────────────────────────────────────────
echo [3/3] 构建 PC Kiosk 安装包...
echo ─────────────────────────────────────────────
cd /d %~dp0..\xnykiosk
if not exist "node_modules" npm install --quiet
npm run build:win
if %ERRORLEVEL% neq 0 (
    echo [WARN] PC 客户端构建失败，跳过
) else (
    echo [OK] PC 客户端构建完成
)

:: ====== 汇总输出 ======
echo.
echo ╔═══════════════════════════════════════════╗
echo ║           构建完成！输出文件汇总          ║
echo ╠═══════════════════════════════════════════╣
echo.

if exist "%~dp0..\tvservice\target\tvservice-0.0.1-SNAPSHOT.jar" (
    echo  ✓ 服务端 JAR：
    echo    tvservice\target\tvservice-0.0.1-SNAPSHOT.jar
) else (
    echo  ✗ 服务端 JAR：构建失败
)

if exist "%~dp0..\tvclient\app\build\outputs\apk\debug\app-debug.apk" (
    echo  ✓ 安卓 APK：
    echo    tvclient\app\build\outputs\apk\debug\app-debug.apk
) else (
    echo  ✗ 安卓 APK：构建失败
)

if exist "%~dp0..\xnykiosk\dist\XNY Kiosk Setup 1.0.0.exe" (
    echo  ✓ PC 安装包：
    echo    xnykiosk\dist\XNY Kiosk Setup 1.0.0.exe
) else if exist "%~dp0..\xnykiosk\dist\" (
    echo  ✓ PC 安装包：
    dir /b "%~dp0..\xnykiosk\dist\*.exe" 2>nul
) else (
    echo  ✗ PC 安装包：构建失败
)

echo.
echo 下一步：阅读 deploy\部署说明.md
echo ╚═══════════════════════════════════════════╝
pause
