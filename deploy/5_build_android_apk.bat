@echo off
chcp 65001 >nul
title 构建 Android TV APK
color 0A

echo ================================================
echo   TV 集中管理系统 - 构建 Android APK
echo ================================================
echo.

set JAVA_HOME=C:\Program Files\Java\jdk-17
set PATH=%JAVA_HOME%\bin;%PATH%

cd /d %~dp0..\tvclient

echo [1/2] 清理旧构建...
call gradlew.bat clean --no-daemon -q

echo [2/2] 构建 Release APK...
call gradlew.bat assembleRelease --no-daemon -q

if %ERRORLEVEL% neq 0 (
    echo.
    echo [ERROR] APK 构建失败！
    echo 尝试构建 Debug APK...
    call gradlew.bat assembleDebug --no-daemon -q
    if %ERRORLEVEL% neq 0 (
        echo [ERROR] Debug APK 也构建失败，请检查项目配置
        pause
        exit /b 1
    )
    echo.
    echo Debug APK 构建成功（适用于内网测试）
    set APK_PATH=%~dp0..\tvclient\app\build\outputs\apk\debug\app-debug.apk
) else (
    set APK_PATH=%~dp0..\tvclient\app\build\outputs\apk\release\app-release-unsigned.apk
)

echo.
echo ================================================
echo   APK 构建成功！
echo   输出文件：%APK_PATH%
echo ================================================
echo.
echo 安装到安卓TV的方式：
echo   方式1（ADB）：adb install "%APK_PATH%"
echo   方式2（U盘）：将 APK 复制到U盘，插到TV上用文件管理器安装
echo   方式3（网络）：将 APK 上传到内网服务器，TV浏览器下载安装
pause
