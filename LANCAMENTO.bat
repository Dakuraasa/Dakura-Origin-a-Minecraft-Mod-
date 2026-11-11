@echo off
chcp 65001 >nul
title Dakura Origins - Preparar lancamento
cd /d "%~dp0projeto\src\main"
echo Removendo racas e arquivos antigos (Anubis, Oni, aureola e cauda antiga da Kitsune)...
del /q "resources\dakura_races\anubis.json" "resources\dakura_races\oni.json" 2>nul
del /q "resources\assets\dakura_origins\textures\race\anubis.png" "resources\assets\dakura_origins\textures\race\oni.png" 2>nul
del /q "java\com\dakura\origins\power\types\DodgePower.java" "java\com\dakura\origins\power\types\SoulGuidePower.java" 2>nul
del /q "java\com\dakura\origins\power\ability\CompassNeedleAbility.java" "java\com\dakura\origins\power\ability\HeartWeighingAbility.java" "java\com\dakura\origins\power\ability\DuatSandsAbility.java" 2>nul
for %%n in (seraph_halo kitsune_tail) do (
    del /q "resources\assets\dakura_origins\geo\%%n.geo.json" 2>nul
    del /q "resources\assets\dakura_origins\animations\%%n.animation.json" 2>nul
    del /q "resources\assets\dakura_origins\textures\entity\geo\%%n.png" 2>nul
)
cd /d "%~dp0"
del /q "REMOVER-ANUBIS-ONI.bat" 2>nul
call COMPILAR.bat
