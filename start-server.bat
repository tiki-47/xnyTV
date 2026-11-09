@echo off
title TV Management Server
set JAVA_HOME=C:\Program Files\Java\jdk-17
set PATH=%JAVA_HOME%\bin;%PATH%

echo ============================================
echo   TV 集中管理系统 - 服务端
echo ============================================
echo   管理页面:  http://localhost:8080
echo   WebSocket: ws://localhost:8080/ws
echo ============================================
echo.
echo 正在启动服务器，请稍候...

cd /d %~dp0tvservice

:: 后台延迟 5 秒后自动打开浏览器
start /b cmd /c "timeout /t 5 /nobreak >nul && start http://localhost:8080"

java -jar target\tvservice-0.0.1-SNAPSHOT.jar

pause
