@echo off
chcp 65001 >nul
title Dakura Origins - Compilar
cd /d "%~dp0projeto"
if not exist "..\logs" mkdir "..\logs"
if not exist "..\builds" mkdir "..\builds"

echo ==== Dakura Origins - build %date% %time% ==== > "..\logs\build-log.txt"
echo. >> "..\logs\build-log.txt"
echo [Java instalado] >> "..\logs\build-log.txt"
java -version >> "..\logs\build-log.txt" 2>&1
echo JAVA_HOME=%JAVA_HOME% >> "..\logs\build-log.txt"
echo. >> "..\logs\build-log.txt"

echo Compilando o Dakura Origins... (a primeira vez demora, baixa o Minecraft e o Fabric)
call gradlew.bat build --no-daemon --stacktrace >> "..\logs\build-log.txt" 2>&1
if errorlevel 1 (
    echo.
    echo  [X] A compilacao FALHOU. O log esta em logs\build-log.txt
    echo      Volte no chat e diga "compilei" que eu leio o log e corrijo.
    echo BUILD_RESULT=FALHOU >> "..\logs\build-log.txt"
    pause
    exit /b 1
)

del /q "..\builds\*.jar" 2>nul
copy /y "build\libs\dakura-origins-*.jar" "..\builds\" >nul
del /q "..\builds\*-sources.jar" 2>nul
echo BUILD_RESULT=OK >> "..\logs\build-log.txt"
echo.
echo  [OK] Pronto! O .jar esta na pasta builds\
echo       Coloque-o na pasta mods do Cobbleverse (junto com o Fabric API).
pause
