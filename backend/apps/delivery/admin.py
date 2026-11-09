from django.contrib import admin

from .models import DeliveryPartnerProfile, DeliveryTracking


@admin.register(DeliveryPartnerProfile)
class DeliveryPartnerProfileAdmin(admin.ModelAdmin):
    list_display = ["user", "vehicle_type", "is_available", "is_verified", "total_deliveries"]
    list_filter = ["vehicle_type", "is_available", "is_verified"]


@admin.register(DeliveryTracking)
class DeliveryTrackingAdmin(admin.ModelAdmin):
    list_display = ["order", "latitude", "longitude", "timestamp"]
