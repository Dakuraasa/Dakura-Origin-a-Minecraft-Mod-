@echo off
chcp 65001 >nul
title Datura Origins - Minecraft de teste
cd /d "%~dp0projeto"
if not exist "..\logs" mkdir "..\logs"
echo Abrindo um Minecraft 1.21.1 de teste so com o Datura Origins...
echo (crie um mundo novo: o livro de origens deve abrir sozinho)
call gradlew.bat runClient --no-daemon > "..\logs\teste-log.txt" 2>&1
echo Minecraft fechado. Log em logs\teste-log.txt
pause
