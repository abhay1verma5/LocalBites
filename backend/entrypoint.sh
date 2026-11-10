#!/bin/bash
set -e

echo "Collecting static files..."
python manage.py collectstatic --no-input || true

echo "Running migrations..."
python manage.py migrate --run-syncdb

echo "Starting gunicorn on port ${PORT:-10000}..."
exec gunicorn localbites.wsgi:application --bind "0.0.0.0:${PORT:-10000}" --timeout 120 --workers 2
