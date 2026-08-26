@ECHO OFF
setlocal enabledelayedexpansion
cd /d "%~dp0"

powershell -NoProfile -ExecutionPolicy Bypass -File "scripts\build_and_version_jar.ps1" %*
if errorlevel 1 (
    echo.
    echo [ERROR] Build that bai!
    pause
    exit /b 1
)

echo.
echo [DONE] San sang chay server bang run.bat!
pause