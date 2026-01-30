@echo off
setlocal enabledelayedexpansion

REM Get the directory where the script is located
set "SCRIPT_DIR=%~dp0"
REM Remove trailing backslash
set "SCRIPT_DIR=%SCRIPT_DIR:~0,-1%"
REM Get parent directory (BASE_DIR)
for %%I in ("%SCRIPT_DIR%\..") do set "BASE_DIR=%%~fI"

REM Build JDBC classpath from multiple sources
set "JDBC_CLASSPATH="

REM 1. Check for dedicated JDBC driver directory (lib\jdbc\)
if exist "%BASE_DIR%\lib\jdbc" (
    set "JDBC_CLASSPATH=%BASE_DIR%\lib\jdbc\*"
)

REM 2. Check for environment variable (can extend or override)
if defined TOTALSCHEMA_JDBC_DRIVERS (
    if defined JDBC_CLASSPATH (
        set "JDBC_CLASSPATH=!JDBC_CLASSPATH!;%TOTALSCHEMA_JDBC_DRIVERS%"
    ) else (
        set "JDBC_CLASSPATH=%TOTALSCHEMA_JDBC_DRIVERS%"
    )
)

REM Build final classpath
if defined JDBC_CLASSPATH (
    set "CLASSPATH=%BASE_DIR%\lib\*;!JDBC_CLASSPATH!"
) else (
    set "CLASSPATH=%BASE_DIR%\lib\*"
)

REM Run the application
java -cp "%CLASSPATH%" io.github.totalschema.cli.Main %*

