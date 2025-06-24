# TeamUP

**TeamUP** is a app built as part of a Master’s project in Software Engineering.

- **Backend:** Laravel microservices 
- **Web frontend:** React  
- **Mobile app:** Android (Kotlin + Jetpack Compose)  
- **Deployment:** Docker Compose

---

## 🚀 Start the server stack

1. Move to the **services** folder:
   ```bash
   cd services
   ```
2. Build *and* start every container:
   ```bash
   docker compose up -d --build
   ```
3. **Install PHP dependencies inside each micro-service** – run **once**, after the first build:
   ```bash
   for s in users-main-app event_manager-app chat-app rating-app achievements-app; do
     docker compose exec "$s" composer install --optimize-autoloader
   done
   ```
4. Run migrations + seeders:
   ```bash
   docker compose exec users-main-app       php artisan migrate --seed
   docker compose exec event_manager-app    php artisan migrate --seed
   docker compose exec chat-app             php artisan migrate --seed
   docker compose exec rating-app           php artisan migrate --seed
   docker compose exec achievements-app     php artisan migrate --seed
   ```
⚠️ **Chat note:** The chat feature does **not** work with seeded activities. You must create a new activity through the app to test the chat functionality.
---
## 🌐 Web Frontend

1. Go to the **FrontEnd/WEB** folder:
   ```bash
   cd FrontEnd/WEB
   ```
2. Start the containers:
   ```bash
   composer install
   docker compose up -d
   ```

3. Access the app in your browser:  
   [http://localhost:3000/](http://localhost:3000/)

## 📱 Mobile Frontend

1. Open the **FrontEnd/Mobile** folder in Android Studio.

## 🧪 Test Account

Use the seeded accounts to log in and test:
- **Email:** `teste1@gmail.com`  
- **Password:** `password`

-  **Email:** `admin@example.com`  
- **Password:** `admin`
