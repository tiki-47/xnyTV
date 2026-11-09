@echo off
chcp 65001 >nul
title 构建服务端 JAR
color 0A

echo ================================================
echo   TV 集中管理系统 - 构建服务端
echo ================================================
echo.

:: 检查 JDK 17
set JAVA_HOME=C:\Program Files\Java\jdk-17
if not exist "%JAVA_HOME%\bin\java.exe" (
    echo [ERROR] 找不到 JDK 17，请先安装 JDK 17 到：
    echo         C:\Program Files\Java\jdk-17
    echo.
    echo 下载地址：https://adoptium.net/
    pause
    exit /b 1
)
set PATH=%JAVA_HOME%\bin;%PATH%

echo [1/2] 检查 JDK 版本...
java -version
echo.

echo [2/2] 编译并打包服务端...
cd /d %~dp0..\tvservice
call mvn clean package -DskipTests -q

if %ERRORLEVEL% neq 0 (
    echo.
    echo [ERROR] 构建失败！请检查上方错误信息
    pause
    exit /b 1
)

echo.
echo ================================================
echo   构建成功！
echo   输出文件：tvservice\target\tvservice-0.0.1-SNAPSHOT.jar
echo ================================================
echo.
echo 下一步：运行 2_setup_database.bat 初始化数据库
pause
