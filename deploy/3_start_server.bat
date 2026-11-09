@echo off
chcp 65001 >nul
title TV 集中管理系统 - 运行中
color 0A

echo ================================================
echo   TV 集中管理系统 - 启动服务端
echo ================================================

set JAVA_HOME=C:\Program Files\Java\jdk-17
set PATH=%JAVA_HOME%\bin;%PATH%

:: JAR 文件路径
set JAR=%~dp0..\tvservice\target\tvservice-0.0.1-SNAPSHOT.jar

if not exist "%JAR%" (
    echo [ERROR] 找不到 JAR 文件，请先运行 1_build_server.bat
    pause
    exit /b 1
)

:: 获取本机 IP（显示用）
for /f "tokens=2 delims=:" %%a in ('ipconfig ^| findstr "IPv4"') do (
    set SERVER_IP=%%a
    goto :found_ip
)
:found_ip
set SERVER_IP=%SERVER_IP: =%

echo.
echo   管理页面：http://%SERVER_IP%:8080
echo   WebSocket：ws://%SERVER_IP%:8080/ws
echo.
echo   TV 端配置时，WS地址填写：ws://%SERVER_IP%:8080/ws
echo   PC 端配置时，WS地址填写：ws://%SERVER_IP%:8080/ws
echo.
echo ================================================
echo   按 Ctrl+C 停止服务
echo ================================================
echo.

:: 延迟打开浏览器
start /b cmd /c "timeout /t 4 /nobreak >nul && start http://localhost:8080"

:: 使用生产环境配置启动（比开发环境更安静，且写日志到 logs/ 目录）
java -jar "%JAR%" --spring.profiles.active=prod

pause
