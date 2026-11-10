#!/bin/bash
echo "Starting gunicorn on port ${PORT:-10000}..."
exec gunicorn localbites.wsgi:application --bind "0.0.0.0:${PORT:-10000}" --timeout 120 --workers 1 --log-level debug
