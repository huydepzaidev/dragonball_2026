@ECHO OFF
cd /d "%~dp0"

:: [DragonBall 2026] Server Runtime JAR
set "JAR=dist\115-NgocRongOnline-20260826.jar"

:: Fallback kiem tra neu file khong ton tai
if not exist "%JAR%" (
    if exist "dist\NgocRongOnline.jar" (
        set "JAR=dist\NgocRongOnline.jar"
    ) else (
        for /f "delims=" %%i in ('dir /b /o-d dist\*.jar 2^>nul') do (
            set "JAR=dist\%%i"
            goto :run_server
        )
    )
)

:run_server
if not exist "%JAR%" (
    echo [ERROR] Khong tim thay JAR runtime de khoi dong server!
    pause
    exit /b 1
)

echo ==========================================================
echo  [DragonBall 2026] Khoi dong server: %JAR%
echo ==========================================================
java -server -Dfile.encoding=UTF-8 -jar "%JAR%"
PAUSE
