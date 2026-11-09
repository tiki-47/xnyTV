@echo off
chcp 65001 >nul
title 初始化数据库
color 0A

echo ================================================
echo   TV 集中管理系统 - 初始化数据库
echo ================================================
echo.
echo 前提：MySQL 8.0 已安装并运行（端口 3306）
echo.

set /p MYSQL_HOST=MySQL 主机地址（本机直接回车默认 localhost）: 
if "%MYSQL_HOST%"=="" set MYSQL_HOST=localhost

set /p MYSQL_USER=MySQL 用户名（默认 root，直接回车）: 
if "%MYSQL_USER%"=="" set MYSQL_USER=root

set /p MYSQL_PASS=MySQL 密码: 

echo.
echo 正在连接 %MYSQL_HOST% 并导入数据库...
echo.

:: 检查 mysql 命令是否可用
where mysql >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo [提示] 未在 PATH 中找到 mysql 命令，尝试常见安装路径...
    set MYSQL_CMD="C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
    if not exist %MYSQL_CMD% (
        echo [ERROR] 找不到 mysql.exe，请确认 MySQL 已安装
        echo         或将 MySQL bin 目录加入系统 PATH
        pause
        exit /b 1
    )
) else (
    set MYSQL_CMD=mysql
)

%MYSQL_CMD% -h %MYSQL_HOST% -u %MYSQL_USER% -p%MYSQL_PASS% < "%~dp0..\database\init.sql"

if %ERRORLEVEL% neq 0 (
    echo.
    echo [ERROR] 数据库初始化失败！
    echo 请检查：1.MySQL 是否运行  2.用户名密码是否正确  3.端口是否为 3306
    pause
    exit /b 1
)

echo.
echo ================================================
echo   数据库初始化成功！数据库名：tv_management
echo ================================================
echo.
echo 下一步：设置环境变量 SPRING_DATASOURCE_PASSWORD 为 MySQL 密码
echo         可选设置 SPRING_DATASOURCE_USERNAME 和 SPRING_DATASOURCE_URL
echo         然后在同一终端运行服务；服务模式需配置服务账号的环境变量
pause
