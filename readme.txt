# README

**Recomendação:** para melhor compatibilidade e desempenho, execute este projeto em ambiente Linux.

## Como correr o servidor

1. Navegue até à pasta **services** e execute:

```
docker compose up -d
```

2. Executar os seeds: basta correr o script **database**, que irá lançar os seguintes comandos:

```
docker compose exec users-main-app php artisan migrate --seed
docker compose exec event_manager-app php artisan migrate --seed
docker compose exec chat-app php artisan migrate --seed
docker compose exec rating-app php artisan migrate --seed
docker compose exec achievements-app php artisan migrate --seed
```

## Como correr o front-end (Web)

1. Navegue até à pasta **FrontEnd/WEB** e execute:

```
docker compose up -d
```

## Como correr o front-end (Mobile)

1. Abra o projecto no Android Studio.

## Testes de funcionalidades

A conta de **seed**:

- **E-mail:** teste1@gmail.com  
- **Password:** password
