# Use the PHP base image with FPM
FROM php:8.2-fpm

# Install system dependencies
RUN apt-get update && apt-get install -y \
    libonig-dev \
    libzip-dev \
    zip \
    unzip \
    git \
    curl \
    supervisor  # Install Supervisor

# Install PHP extensions
RUN docker-php-ext-install pdo_mysql mbstring zip exif pcntl bcmath sockets

# Install Redis PHP extension
RUN pecl install redis && docker-php-ext-enable redis

# Install Composer globally
COPY --from=composer:latest /usr/bin/composer /usr/bin/composer

# Set working directory
WORKDIR /var/www

# Copy application files
COPY . /var/www

# Install PHP dependencies
RUN composer install

# Add tymon/jwt-auth dependency
RUN composer require tymon/jwt-auth

# Publish JWT configuration
RUN php artisan vendor:publish --provider="Tymon\JWTAuth\Providers\LaravelServiceProvider"

# Generate JWT secret key
RUN php artisan jwt:secret

# Ensure storage/logs/laravel.log exists and has correct permissions
RUN mkdir -p storage/logs && \
    touch storage/logs/laravel.log && \
    chmod -R 775 storage bootstrap/cache storage/logs && \
    chown -R www-data:www-data storage bootstrap/cache storage/logs

# Create necessary directories for Supervisor
RUN mkdir -p /var/log/supervisor && \
    mkdir -p /var/run && \
    chmod -R 777 /var/run && \
    chmod -R 777 /var/log/supervisor

# Expose the application on port 9000
EXPOSE 9000

# Copy the Supervisor configuration file into the container
COPY ./supervisord.conf /etc/supervisor/conf.d/supervisord.conf

# Copy and enable the entrypoint script
COPY docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh
RUN chmod +x /usr/local/bin/docker-entrypoint.sh

# Set the entrypoint
ENTRYPOINT ["/usr/local/bin/docker-entrypoint.sh"]

# Start Supervisor as the container's main process
CMD ["/usr/bin/supervisord", "-c", "/etc/supervisor/conf.d/supervisord.conf"]
