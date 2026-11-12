@echo off
chcp 65001 >nul
title Datura Origins - Compilar
cd /d "%~dp0projeto"
rem arquivos de raças/poderes removidos (Serafim e a passiva Sorye) - apaga as sobras antigas
del /q "src\main\resources\dakura_races\serafim.json" 2>nul
del /q "src\main\resources\assets\dakura_origins\textures\race\serafim.png" 2>nul
del /q "src\main\java\com\dakura\origins\power\types\SoryePower.java" 2>nul
del /q "src\main\java\com\dakura\origins\power\ability\SeraphBlessingAbility.java" 2>nul
del /q "src\main\java\com\dakura\origins\power\ability\CelestialAltarAbility.java" 2>nul
del /q "src\client\java\com\dakura\origins\client\screen\SeraphAltarScreen.java" 2>nul
if not exist "..\logs" mkdir "..\logs"
if not exist "..\builds" mkdir "..\builds"

echo ==== Datura Origins - build %date% %time% ==== > "..\logs\build-log.txt"
echo. >> "..\logs\build-log.txt"
echo [Java instalado] >> "..\logs\build-log.txt"
java -version >> "..\logs\build-log.txt" 2>&1
echo JAVA_HOME=%JAVA_HOME% >> "..\logs\build-log.txt"
echo. >> "..\logs\build-log.txt"

echo Compilando o Datura Origins... (a primeira vez demora, baixa o Minecraft e o Fabric)
call gradlew.bat build --no-daemon --stacktrace >> "..\logs\build-log.txt" 2>&1
if errorlevel 1 (
    echo.
    echo  [X] A compilacao FALHOU. O log esta em logs\build-log.txt
    echo      Volte no chat e diga "compilei" que eu leio o log e corrijo.
    echo BUILD_RESULT=FALHOU >> "..\logs\build-log.txt"
    pause
    exit /b 1
)

copy /y "build\libs\datura-origins-*.jar" "..\builds\" >nul
del /q "..\builds\*-sources.jar" 2>nul
echo BUILD_RESULT=OK >> "..\logs\build-log.txt"
echo.
echo  [OK] Pronto! O .jar esta na pasta builds\
echo       Coloque-o na pasta mods do Cobbleverse (junto com o Fabric API).
timeout /t 10
