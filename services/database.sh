#!/usr/bin/env bash
set -euo pipefail

# Full service names as in your docker-compose.yml
SERVICES=(
  "users-main-app"
  "event_manager-app"
  "chat-app"
  "rating-app"
  "achievements-app"
)

echo "🛠️  Running migrations + seeders in order…"
for svc in "${SERVICES[@]}"; do
  echo "→ ${svc}"
  docker compose exec "${svc}" php artisan migrate --seed
done

echo "✅  All migrations & seeders completed!"
