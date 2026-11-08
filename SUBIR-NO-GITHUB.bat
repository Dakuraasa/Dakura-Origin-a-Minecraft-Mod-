@echo off
chcp 65001 >nul
title Dakura Origins - Enviar para o GitHub (versao + release)
cd /d "%~dp0"
where git >nul 2>&1 || (echo Git nao encontrado. Instale em https://git-scm.com/download/win e rode de novo. & pause & exit /b 1)
for /f "tokens=2 delims==" %%v in ('findstr /b "mod_version" projeto\gradle.properties') do set "VER=%%v"
if not exist ".git" git init -b main
git config user.name >nul 2>&1 || git config user.name "Dakuraasa"
git config user.email >nul 2>&1 || git config user.email "Dakuraasa@users.noreply.github.com"
git add -A
git commit -m "Dakura Origins %VER%"
git remote remove origin >nul 2>&1
git remote add origin https://github.com/Dakuraasa/Dakura-Origin-a-Minecraft-Mod-.git
if not exist logs mkdir logs
git pull origin main --allow-unrelated-histories --no-edit -X ours > logs\github-log.txt 2>&1
git push -u origin main >> logs\github-log.txt 2>&1
type logs\github-log.txt
echo.
where gh >nul 2>&1 && (gh release create v%VER% "builds\dakura-origins-%VER%.jar" --title "Dakura Origins %VER% (Minecraft 1.21.1 / Fabric)" --notes "Versao oficial: 13 racas - Humano, Vampirica, Draconica, Espectro, Driade, Elfo, Huli Jing, Cabra, Minotauro, Gargula, Morto-vivo, Demonio e Vastaya.") || echo Para o .jar em Releases, crie a release v%VER% pelo site do GitHub e anexe builds\dakura-origins-%VER%.jar.
echo.
echo Para as proximas atualizacoes, use o ATUALIZAR-GITHUB.bat (um clique).
pause
