# Nome do Projeto

**Recomendação:** para melhor compatibilidade e desempenho, execute este projeto em ambiente Linux.

## 🚀 Iniciar o Servidor

1. Navegue até à pasta **services**:
   ```bash
   cd services
   ```
2. Suba os containers em modo detached:
   ```bash
   docker compose up -d
   ```
3. Execute os seeds através do script **database**, que irá lançar os comandos:
   ```bash
   docker compose exec users-main-app       php artisan migrate --seed
   docker compose exec event_manager-app    php artisan migrate --seed
   docker compose exec chat-app             php artisan migrate --seed
   docker compose exec rating-app           php artisan migrate --seed
   docker compose exec achievements-app     php artisan migrate --seed
   ```

## 🌐 Front-end (Web)

1. Navegue até à pasta **FrontEnd/WEB**:
   ```bash
   cd FrontEnd/WEB
   ```
2. Suba os containers:
   ```bash
   docker compose up -d
   ```

## 📱 Front-end (Mobile)

1. Abra o pasta **FrontEnd/Mobile** no Android Studio.

## 🧪 Testes de Funcionalidades

Utilize a conta de **seed** para aceder e testar:
- **E-mail:** `teste1@gmail.com`
- **Password:** `password`
