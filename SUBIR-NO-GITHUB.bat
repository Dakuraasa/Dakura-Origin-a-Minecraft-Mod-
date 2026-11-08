@echo off
chcp 65001 >nul
title Dakura Origins - Enviar para o GitHub
cd /d "%~dp0"
where git >nul 2>&1 || (echo Git nao encontrado. Instale em https://git-scm.com/download/win e rode de novo. & pause & exit /b 1)
if not exist ".git" git init -b main
git config user.name >nul 2>&1 || git config user.name "Dakuraasa"
git config user.email >nul 2>&1 || git config user.email "Dakuraasa@users.noreply.github.com"
git add -A
git commit -m "Dakura Origins 0.1.0"
git remote remove origin >nul 2>&1
git remote add origin https://github.com/Dakuraasa/Dakura-Origin-a-Minecraft-Mod-.git
if not exist logs mkdir logs
git pull origin main --allow-unrelated-histories --no-edit -X ours > logs\github-log.txt 2>&1
git push -u origin main >> logs\github-log.txt 2>&1
type logs\github-log.txt
echo.
where gh >nul 2>&1 && (gh release create v0.1.0 "builds\dakura-origins-0.1.0.jar" --title "Dakura Origins 0.1.0 (Minecraft 1.21.1 / Fabric)" --notes "Primeira versao: Humano, Vampirica, Draconica, Espectro, Driade e Elfo.") || echo Para o .jar em Releases, crie a release pelo site do GitHub.
echo.
pause
