@echo off
chcp 65001 >nul
title Dakura Origins - Atualizar o GitHub
cd /d "%~dp0"
where git >nul 2>&1 || (echo Git nao encontrado. Instale em https://git-scm.com/download/win e rode de novo. & pause & exit /b 1)
if not exist ".git" (
    echo Este e o primeiro envio? Rode o SUBIR-NO-GITHUB.bat uma vez.
    pause
    exit /b 1
)
if not exist logs mkdir logs

rem Mensagem: argumento do comando, ou digitada, ou automatica com data e hora
set "MSG=%~1"
if "%MSG%"=="" set /p "MSG=Descreva a mudanca (Enter = mensagem automatica): "
if "%MSG%"=="" set "MSG=Atualizacao %date% %time:~0,5%"

git add -A
git diff --cached --quiet && echo Nada novo para enviar. && timeout /t 3 >nul && exit /b 0
git commit -m "%MSG%" >nul
echo Enviando "%MSG%"...
git pull origin main --no-edit -X ours > logs\github-log.txt 2>&1
git push origin main >> logs\github-log.txt 2>&1
if errorlevel 1 (
    echo.
    echo  [X] Nao consegui enviar. Detalhes:
    type logs\github-log.txt
    pause
    exit /b 1
)
echo.
echo  [OK] Repositorio atualizado: https://github.com/Dakuraasa/Dakura-Origin-a-Minecraft-Mod-
timeout /t 4 >nul
