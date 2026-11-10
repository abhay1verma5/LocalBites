import dj_database_url

from .base import *  # noqa: F401, F403

DEBUG = False

ALLOWED_HOSTS = config("ALLOWED_HOSTS", default="*").split(",")

CORS_ALLOW_ALL_ORIGINS = True

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

# Redis is optional on free tier — use in-memory channel layer if not available
REDIS_HOST = config("REDIS_HOST", default="")
if REDIS_HOST:
    CHANNEL_LAYERS = {
        "default": {
            "BACKEND": "channels_redis.core.RedisChannelLayer",
            "CONFIG": {
                "hosts": [(REDIS_HOST, 6379)],
            },
        },
    }
else:
    CHANNEL_LAYERS = {
        "default": {
            "BACKEND": "channels.layers.InMemoryChannelLayer",
        },
    }

# Celery — optional, tasks run synchronously if no broker
CELERY_BROKER_URL = config("CELERY_BROKER_URL", default="")
CELERY_RESULT_BACKEND = config("CELERY_RESULT_BACKEND", default="")
if not CELERY_BROKER_URL:
    CELERY_TASK_ALWAYS_EAGER = True

# Security
SECURE_BROWSER_XSS_FILTER = True
SECURE_CONTENT_TYPE_NOSNIFF = True

# WhiteNoise for static files
STORAGES = {
    "staticfiles": {
        "BACKEND": "whitenoise.storage.CompressedManifestStaticFilesStorage",
    },
}
