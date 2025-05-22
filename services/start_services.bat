@echo off
setlocal enabledelayedexpansion

REM Ordem definida dos microserviços (nomes de pasta reais)
set servicos=users-main event_manager chat notifications rabbit achievements

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

echo 🛠️ A correr migrations na mesma ordem...

for %%s in (%servicos%) do (
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
