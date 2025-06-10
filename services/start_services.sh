#!/bin/bash

# Ordem definida dos microserviços (nomes de pasta reais)
servicos=("users-main" "event_manager" "chat" "notifications" "rabbit" "achievements" "rating" "websocket" "API_Gateway")

# Apenas os serviços Laravel que requerem migrate e composer install
laravel_servicos=("users-main" "event_manager" "chat" "notifications" "achievements" "rating")

echo "🚀 A iniciar microserviços na ordem definida..."

for s in "${servicos[@]}"; do
    if [ -f "$s/docker-compose.yml" ]; then
        echo "A preparar $s..."
        cd "$s"

        echo "🚢 A iniciar $s..."
        docker compose up -d

        # Se o serviço for Laravel, corre o composer install dentro do container
        if [[ " ${laravel_servicos[*]} " == *" $s "* ]]; then
            echo "📦 A correr 'composer install' dentro do container em $s..."
            docker compose exec app composer install
        fi

        cd ..
    else
        echo "⚠️  Pasta $s ou docker-compose.yml não encontrado. A saltar..."
    fi
done

echo "🛠️ A correr migrations nos serviços Laravel..."

for s in "${laravel_servicos[@]}"; do
    if [ -f "$s/docker-compose.yml" ]; then
        echo "🧬 Migrating $s..."
        cd "$s"
        docker compose exec app php artisan migrate --seed
        cd ..
    else
        echo "⚠️  Pasta $s ou docker-compose.yml não encontrado. A saltar..."
    fi
done

echo "✅ Todos os serviços foram iniciados, preparados e migrados com sucesso!"
