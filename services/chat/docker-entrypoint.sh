#!/bin/sh
set -e

# Fix permissions for Laravel writable directories
chown -R www-data:www-data storage bootstrap/cache

# Execute whatever CMD is in the Dockerfile
exec "$@"
