FROM python:3.12-slim

ENV PYTHONDONTWRITEBYTECODE=1
ENV PYTHONUNBUFFERED=1

WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends \
    gcc libpq-dev && \
    rm -rf /var/lib/apt/lists/*

COPY requirements-prod.txt .
RUN pip install --no-cache-dir -r requirements-prod.txt

COPY . .

RUN DJANGO_SETTINGS_MODULE=localbites.settings.prod \
    SECRET_KEY=build-only-key \
    DATABASE_URL=sqlite:///dev.db \
    python manage.py collectstatic --no-input

EXPOSE 10000

CMD ["sh", "-c", "python manage.py migrate && gunicorn localbites.wsgi:application --bind 0.0.0.0:${PORT:-10000}"]
