@echo off
setlocal enabledelayedexpansion

REM Get the directory where the script is located
set "SCRIPT_DIR=%~dp0"
REM Remove trailing backslash
set "SCRIPT_DIR=%SCRIPT_DIR:~0,-1%"
REM Get parent directory (BASE_DIR)
for %%I in ("%SCRIPT_DIR%\..") do set "BASE_DIR=%%~fI"

REM Check if Java is available
where java >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Java is not found in PATH >&2
    echo.
    echo TotalSchema requires Java 11 or higher to run.
    echo Please install Java and ensure it is available in your PATH.
    echo.
    echo To install Java on Windows:
    echo   - Download from: https://adoptium.net/
    echo   - Or use: winget install EclipseAdoptium.Temurin.11
    echo.
    echo After installation, you may need to restart your terminal.
    exit /b 1
)

REM Build classpath starting with bundled libraries
set "CLASSPATH=%BASE_DIR%\lib\*"

REM Add user_libs directory if it exists
if exist "%BASE_DIR%\user_libs" (
    set "CLASSPATH=!CLASSPATH!;%BASE_DIR%\user_libs\*"
)

REM Add TOTALSCHEMA_USER_LIBS environment variable if set
if defined TOTALSCHEMA_USER_LIBS (
    set "CLASSPATH=!CLASSPATH!;%TOTALSCHEMA_USER_LIBS%"
)

REM Run the application
java -cp "%CLASSPATH%" io.github.totalschema.cli.Main %*

