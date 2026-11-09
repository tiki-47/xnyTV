@echo off
chcp 65001 >nul
title 安装 Windows 服务
color 0A

:: 必须以管理员身份运行
net session >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo [ERROR] 请右键本脚本，选择"以管理员身份运行"
    pause
    exit /b 1
)

echo ================================================
echo   TV 集中管理系统 - 安装为 Windows 服务
echo   （开机自动启动，无需登录，后台运行）
echo ================================================
echo.

set JAVA_HOME=C:\Program Files\Java\jdk-17
set JAR_PATH=%~dp0..\tvservice\target\tvservice-0.0.1-SNAPSHOT.jar
set SERVICE_NAME=TVManagementServer
set SERVICE_DISPLAY=TV集中管理系统

:: 检查 JAR
if not exist "%JAR_PATH%" (
    echo [ERROR] 找不到 JAR 文件，请先运行 1_build_server.bat
    pause
    exit /b 1
)

:: 获取绝对路径
for %%i in ("%JAR_PATH%") do set JAR_ABS=%%~fi

echo [1/3] 检查是否已安装服务...
sc query %SERVICE_NAME% >nul 2>&1
if %ERRORLEVEL% equ 0 (
    echo 服务已存在，先停止并删除...
    sc stop %SERVICE_NAME% >nul 2>&1
    timeout /t 3 /nobreak >nul
    sc delete %SERVICE_NAME%
    timeout /t 2 /nobreak >nul
)

echo [2/3] 注册 Windows 服务...
sc create %SERVICE_NAME% ^
    binPath= "\"%JAVA_HOME%\bin\java.exe\" -jar \"%JAR_ABS%\" --spring.profiles.active=prod" ^
    DisplayName= "%SERVICE_DISPLAY%" ^
    start= auto ^
    obj= LocalSystem

if %ERRORLEVEL% neq 0 (
    echo [ERROR] 服务注册失败
    pause
    exit /b 1
)

:: 添加服务描述
sc description %SERVICE_NAME% "TV看板集中管理系统，提供WebSocket设备管理和Web管理界面"

echo [3/3] 启动服务...
sc start %SERVICE_NAME%

timeout /t 5 /nobreak >nul

sc query %SERVICE_NAME% | findstr "RUNNING" >nul
if %ERRORLEVEL% equ 0 (
    echo.
    echo ================================================
    echo   服务安装并启动成功！
    echo   服务名：%SERVICE_NAME%
    echo   管理页面：http://localhost:8080
    echo   开机自动启动：已启用
    echo ================================================
    echo.
    echo 管理命令：
    echo   启动：sc start %SERVICE_NAME%
    echo   停止：sc stop %SERVICE_NAME%
    echo   删除：sc delete %SERVICE_NAME%
) else (
    echo.
    echo [WARN] 服务已注册但未能立即启动
    echo 请检查：事件查看器 → Windows日志 → 应用程序
    echo 或查看：tvservice\logs\tvservice.log
)
pause
