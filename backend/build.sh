#!/usr/bin/env bash
set -o errexit

echo "=== Python version ==="
python --version

echo "=== Current directory ==="
pwd
ls -la

echo "=== Installing dependencies ==="
pip install --upgrade pip
pip install -r requirements-prod.txt

echo "=== Setting up Django ==="
export DJANGO_SETTINGS_MODULE=localbites.settings.prod

echo "=== Collecting static files ==="
python manage.py collectstatic --no-input

echo "=== Running migrations ==="
python manage.py migrate

echo "=== Build complete ==="
