import dj_database_url

from .base import *  # noqa: F401, F403

DEBUG = False

ALLOWED_HOSTS = config("ALLOWED_HOSTS", default="*").split(",")

CORS_ALLOW_ALL_ORIGINS = True

# Remove daphne/channels from INSTALLED_APPS for production (not needed with gunicorn)
INSTALLED_APPS = [app for app in INSTALLED_APPS if app not in ("daphne",)]

# Use DATABASE_URL from Render (or fall back to individual vars)
DATABASE_URL = config("DATABASE_URL", default="")
if DATABASE_URL:
    DATABASES = {
        "default": dj_database_url.parse(DATABASE_URL, conn_max_age=600),
    }
else:
    DATABASES = {
        "default": {
            "ENGINE": "django.db.backends.postgresql",
            "NAME": config("DB_NAME"),
            "USER": config("DB_USER"),
            "PASSWORD": config("DB_PASSWORD"),
            "HOST": config("DB_HOST"),
            "PORT": config("DB_PORT", default="5432"),
        }
    }

# No channels/redis on free tier
CHANNEL_LAYERS = {}

# No Celery on free tier
CELERY_TASK_ALWAYS_EAGER = True

# Use WSGI (gunicorn), not ASGI
WSGI_APPLICATION = "localbites.wsgi.application"

# Security
SECURE_BROWSER_XSS_FILTER = True
SECURE_CONTENT_TYPE_NOSNIFF = True

# WhiteNoise for static files
STORAGES = {
    "staticfiles": {
        "BACKEND": "whitenoise.storage.CompressedManifestStaticFilesStorage",
    },
}
