@echo off
chcp 65001 >nul
title Datura Origins - Enviar para o GitHub (versao + release)
cd /d "%~dp0"
where git >nul 2>&1 || (echo Git nao encontrado. Instale em https://git-scm.com/download/win e rode de novo. & pause & exit /b 1)
for /f "tokens=2 delims==" %%v in ('findstr /b "mod_version" projeto\gradle.properties') do set "VER=%%v"
set "REPO=Dakuraasa/Datura-Origins-a-Minecraft-Mod"
if not exist logs mkdir logs
rem nome novo do repositorio (o GitHub redireciona o link antigo); se ja foi renomeado, so segue
where gh >nul 2>&1 && gh repo rename Datura-Origins-a-Minecraft-Mod -R Dakuraasa/Dakura-Origin-a-Minecraft-Mod- --yes > logs\github-log.txt 2>&1
if not exist ".git" git init -b main
git config user.name >nul 2>&1 || git config user.name "Daturaasa"
git config user.email >nul 2>&1 || git config user.email "Dakuraasa@users.noreply.github.com"
git add -A
git commit -m "Datura Origins %VER%"
git remote remove origin >nul 2>&1
git remote add origin https://github.com/%REPO%.git
git pull origin main --allow-unrelated-histories --no-edit -X ours >> logs\github-log.txt 2>&1
git push -u origin main >> logs\github-log.txt 2>&1
type logs\github-log.txt
echo.
where gh >nul 2>&1 || (echo gh nao encontrado: crie a release v%VER% pelo site e anexe builds\datura-origins-%VER%.jar. & pause & exit /b 1)
set "JAR=builds\datura-origins-%VER%.jar"
if not exist "%JAR%" (echo Nao achei %JAR%. Rode o COMPILAR.bat antes. & pause & exit /b 1)
gh release view v%VER% -R %REPO% >nul 2>&1
if errorlevel 1 (
    gh release create v%VER% "%JAR%" -R %REPO% --title "Datura Origins %VER% (Minecraft 1.21.1 / Fabric)" --notes "Datura Origins %VER% para Minecraft 1.21.1 (Fabric). Requer Fabric API. Criado por Daturaasa." >> logs\github-log.txt 2>&1
) else (
    gh release upload v%VER% "%JAR%" -R %REPO% --clobber >> logs\github-log.txt 2>&1
)
if errorlevel 1 (echo  [X] A release falhou. Detalhes: & type logs\github-log.txt & pause & exit /b 1)
echo  [OK] Release v%VER% publicada com %JAR%
echo.
echo Apagando as releases antigas (fica so a v%VER%)...
for /f "delims=" %%t in ('gh release list -R %REPO% --limit 200 --json tagName --jq ".[].tagName"') do (
    if /i not "%%t"=="v%VER%" (
        echo  - apagando %%t
        gh release delete %%t -R %REPO% --yes --cleanup-tag >> logs\github-log.txt 2>&1
    )
)
echo.
echo Releases que ficaram:
gh release list -R %REPO% > logs\github-releases.txt 2>&1
type logs\github-releases.txt
echo.
echo Para as proximas atualizacoes, use o ATUALIZAR-GITHUB.bat (um clique).
pause
