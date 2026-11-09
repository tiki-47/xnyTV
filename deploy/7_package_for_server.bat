@echo off
chcp 65001 >nul
title 打包服务端部署包
color 0A

echo ================================================
echo   打包服务端部署包（拷贝到目标服务器使用）
echo ================================================
echo.

set OUTPUT_DIR=%~dp0server_package

:: 创建输出目录
if exist "%OUTPUT_DIR%" rd /s /q "%OUTPUT_DIR%"
mkdir "%OUTPUT_DIR%"
mkdir "%OUTPUT_DIR%\logs"

set JAR=%~dp0..\tvservice\target\tvservice-0.0.1-SNAPSHOT.jar
if not exist "%JAR%" (
    echo [ERROR] 找不到 JAR，请先运行 1_build_server.bat
    pause
    exit /b 1
)

echo 正在打包...

:: 复制 JAR
copy "%JAR%" "%OUTPUT_DIR%\tvservice.jar" >nul

:: 复制生产配置
copy "%~dp0..\tvservice\src\main\resources\application-prod.yml" "%OUTPUT_DIR%\" >nul

:: 创建启动脚本（目标服务器上使用）
(
echo @echo off
echo chcp 65001 ^>nul
echo title TV 集中管理系统
echo set JAVA_HOME=C:\Program Files\Java\jdk-17
echo set PATH=%%JAVA_HOME%%\bin;%%PATH%%
echo.
echo echo ================================
echo echo TV 集中管理系统启动中...
echo echo 管理页面：http://localhost:8080
echo echo ================================
echo.
echo start /b cmd /c "timeout /t 5 /nobreak ^>nul ^&^& start http://localhost:8080"
echo java -jar "%%~dp0tvservice.jar" --spring.config.location="%%~dp0application-prod.yml"
echo pause
) > "%OUTPUT_DIR%\启动服务.bat"

:: 创建服务安装脚本
(
echo @echo off
echo net session ^>nul 2^>^&1
echo if %%ERRORLEVEL%% neq 0 ^(echo 请以管理员身份运行 ^&^& pause ^&^& exit /b^)
echo set JAVA=C:\Program Files\Java\jdk-17\bin\java.exe
echo set JAR=%%~dp0tvservice.jar
echo set CFG=%%~dp0application-prod.yml
echo sc create TVManagementServer binPath= "\"%%JAVA%%\" -jar \"%%JAR%%\" --spring.config.location=\"%%CFG%%\"" DisplayName= "TV集中管理系统" start= auto
echo sc description TVManagementServer "TV看板集中管理，WebSocket设备控制"
echo sc start TVManagementServer
echo echo 服务已安装并启动！
echo pause
) > "%OUTPUT_DIR%\安装为Windows服务(管理员运行).bat"

:: 创建 README
(
echo TV 集中管理系统 - 服务端部署包
echo ================================
echo.
echo 【前提条件】（目标 Windows 服务器需要安装）
echo   1. JDK 17：https://adoptium.net/temurin/releases/?version=17
echo   2. MySQL 8.0：https://dev.mysql.com/downloads/installer/
echo.
echo 【部署步骤】
echo   1. 安装 JDK 17 到 C:\Program Files\Java\jdk-17
echo   2. 安装 MySQL 8.0，记住 root 密码
echo   3. 导入数据库：mysql -u root -p ^< init.sql
echo      （init.sql 在项目 database 目录，需手动复制过来）
echo   4. 设置环境变量 SPRING_DATASOURCE_PASSWORD 为 MySQL 密码
echo      服务模式需为服务账号配置此变量；可另设 SPRING_DATASOURCE_USERNAME
echo   5. 运行"安装为Windows服务"（推荐）或"启动服务"
echo.
echo 【防火墙】以管理员运行 PowerShell 执行：
echo   netsh advfirewall firewall add rule name="TV管理系统" dir=in action=allow protocol=TCP localport=8080
echo.
echo 【管理页面】http://本机IP:8080
echo 【WebSocket】ws://本机IP:8080/ws
) > "%OUTPUT_DIR%\README.txt"

echo.
echo ================================================
echo   打包完成！
echo   输出目录：%OUTPUT_DIR%
echo.
echo   将此目录复制到目标服务器，按 README.txt 操作
echo ================================================

:: 打开输出目录
explorer "%OUTPUT_DIR%"
pause
