@echo off
title Build TV Management Server
set JAVA_HOME=C:\Program Files\Java\jdk-17
set PATH=%JAVA_HOME%\bin;%PATH%

echo Building TV Management Server...
cd /d %~dp0tvservice
call mvn clean package -DskipTests

if %ERRORLEVEL% == 0 (
    echo.
    echo BUILD SUCCESS!
    echo JAR: tvservice\target\tvservice-0.0.1-SNAPSHOT.jar
) else (
    echo.
    echo BUILD FAILED!
)
pause
