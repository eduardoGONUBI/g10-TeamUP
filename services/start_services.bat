@echo off
setlocal enabledelayedexpansion

REM Diretórios dos microserviços (ajustados aos nomes reais)
set services=auth API_Gateway chat notifications event_manager rating achievements users-main websocket

REM Microserviços que precisam de migrate --seed
set needs_migration=notifications event_manager rating achievements users-main chat

echo 🚀 A iniciar todos os microserviços...

for %%s in (%services%) do (
    if exist "%%s\docker-compose.yml" (
        echo Iniciando %%s...
        cd %%s
        docker compose up -d
        cd ..
    ) else (
        echo ⚠️  Pasta %%s ou docker-compose.yml não encontrado. A saltar...
    )
)

echo 🛠️ A correr migrations nos serviços necessários...

for %%m in (%needs_migration%) do (
    if exist "%%m\docker-compose.yml" (
        echo Migrating %%m...
        cd %%m
        docker compose exec app php artisan migrate --seed
        cd ..
    ) else (
        echo ⚠️  Pasta %%m ou docker-compose.yml não encontrado. A saltar...
    )
)

echo ✅ Todos os serviços foram iniciados com sucesso!
pause
