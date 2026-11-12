@echo off
chcp 65001 >nul
title Datura Origins - Atualizar o GitHub
cd /d "%~dp0"
where git >nul 2>&1 || (echo Git nao encontrado. Instale em https://git-scm.com/download/win e rode de novo. & pause & exit /b 1)
if not exist ".git" (
    echo Este e o primeiro envio? Rode o SUBIR-NO-GITHUB.bat uma vez.
    pause
    exit /b 1
)
if not exist logs mkdir logs

rem Uma juncao que parou no meio (conflito): fica com os arquivos deste computador e conclui
if exist ".git\MERGE_HEAD" (
    echo Concluindo a juncao pendente com os arquivos deste computador...
    git add -A
    git commit --no-edit >nul
)

rem Mensagem: argumento do comando, ou digitada, ou automatica com data e hora
set "MSG=%~1"
if "%MSG%"=="" set /p "MSG=Descreva a mudanca (Enter = mensagem automatica): "
if "%MSG%"=="" set "MSG=Atualizacao %date% %time:~0,5%"

git add -A
git diff --cached --quiet || git commit -m "%MSG%" >nul
echo Enviando...
git pull origin main --no-edit -X ours > logs\github-log.txt 2>&1
if exist ".git\MERGE_HEAD" (
    rem conflito que o git nao resolve sozinho (ex.: arquivo apagado de um lado): vale a versao deste computador
    git add -A
    git commit --no-edit >> logs\github-log.txt 2>&1
)
git push origin main >> logs\github-log.txt 2>&1
if errorlevel 1 (
    echo.
    echo  [X] Nao consegui enviar. Detalhes:
    type logs\github-log.txt
    pause
    exit /b 1
)
echo.
echo  [OK] Repositorio atualizado: https://github.com/Dakuraasa/Datura-Origins-a-Minecraft-Mod
timeout /t 5 >nul
