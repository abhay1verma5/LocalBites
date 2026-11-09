import os

from celery import Celery

os.environ.setdefault("DJANGO_SETTINGS_MODULE", "localbites.settings.dev")

app = Celery("localbites")
app.config_from_object("django.conf:settings", namespace="CELERY")
app.autodiscover_tasks()
