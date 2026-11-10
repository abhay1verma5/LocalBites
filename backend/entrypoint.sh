#!/bin/bash

echo "Collecting static files..."
python manage.py collectstatic --no-input 2>&1 || true

echo "Running migrations..."
python manage.py migrate 2>&1 || echo "WARNING: Migrations failed, continuing..."

echo "Starting gunicorn on port ${PORT:-10000}..."
exec gunicorn localbites.wsgi:application --bind "0.0.0.0:${PORT:-10000}" --timeout 120 --workers 2
