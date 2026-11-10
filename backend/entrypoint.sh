#!/bin/bash
set -e

echo "Running collectstatic..."
python manage.py collectstatic --no-input 2>&1 || echo "collectstatic failed, continuing..."

echo "Running migrations..."
python manage.py migrate 2>&1 || echo "migrate failed, continuing..."

echo "Starting gunicorn on port ${PORT:-10000}..."
exec gunicorn localbites.wsgi:application --bind "0.0.0.0:${PORT:-10000}" --timeout 120 --workers 2
