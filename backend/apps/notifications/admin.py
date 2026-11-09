from django.contrib import admin

from .models import FCMDevice, Notification


@admin.register(FCMDevice)
class FCMDeviceAdmin(admin.ModelAdmin):
    list_display = ["user", "device_type", "is_active", "created_at"]


@admin.register(Notification)
class NotificationAdmin(admin.ModelAdmin):
    list_display = ["user", "title", "is_read", "created_at"]
    list_filter = ["is_read"]
