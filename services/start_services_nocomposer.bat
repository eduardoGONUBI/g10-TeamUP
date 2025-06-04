@echo off
setlocal enabledelayedexpansion

REM Ordem definida dos microserviços (nomes de pasta reais)
set servicos=users-main event_manager chat notifications rabbit achievements rating websocket API_Gateway

REM Apenas os serviços Laravel que requerem migrate
set laravel_servicos=users-main event_manager chat notifications achievements rating

echo 🚀 A iniciar microserviços na ordem definida...

for %%s in (%servicos%) do (
    if exist "%%s\docker-compose.yml" (
        echo Iniciando %%s...
        cd %%s
        docker compose up -d
        cd ..
    ) else (
        echo ⚠️  Pasta %%s ou docker-compose.yml não encontrado. A saltar...
    )
)

echo 🛠️ A correr migrations nos serviços Laravel...

for %%s in (%laravel_servicos%) do (
    if exist "%%s\docker-compose.yml" (
        echo Migrating %%s...
        cd %%s
        docker compose exec app php artisan migrate --seed
        cd ..
    ) else (
        echo ⚠️  Pasta %%s ou docker-compose.yml não encontrado. A saltar...
    )
)

echo ✅ Todos os serviços foram iniciados e migrados com sucesso!
pause
