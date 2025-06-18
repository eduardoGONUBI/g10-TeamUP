@echo off
REM Sai imediatamente em caso de erro
SETLOCAL ENABLEDELAYEDEXPANSION

REM Lista de serviços conforme definidos no docker-compose.yml
SET "SERVICES=users-main-app event_manager-app chat-app rating-app achievements-app"

echo 🛠️  A executar migrations + seeders por ordem…

for %%S in (%SERVICES%) do (
    echo → %%S
    docker compose exec %%S php artisan migrate --seed
    if errorlevel 1 (
        echo ❌  Erro ao executar migrations em %%S. Abortando.
        EXIT /b 1
    )
)

echo ✅  Todas as migrations & seeders concluídas!
ENDLOCAL
