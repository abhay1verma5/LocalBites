from django.conf import settings
from django.db import models

from apps.orders.models import Order


class DeliveryPartnerProfile(models.Model):
    class VehicleType(models.TextChoices):
        BICYCLE = "BICYCLE", "Bicycle"
        MOTORCYCLE = "MOTORCYCLE", "Motorcycle"
        SCOOTER = "SCOOTER", "Scooter"

    user = models.OneToOneField(
        settings.AUTH_USER_MODEL,
        on_delete=models.CASCADE,
        related_name="delivery_profile",
    )
    vehicle_type = models.CharField(max_length=15, choices=VehicleType.choices)
    vehicle_number = models.CharField(max_length=20, blank=True)
    license_number = models.CharField(max_length=30, blank=True)
    is_available = models.BooleanField(default=False)
    is_verified = models.BooleanField(default=False)
    current_latitude = models.DecimalField(
        max_digits=10, decimal_places=7, null=True, blank=True
    )
    current_longitude = models.DecimalField(
        max_digits=10, decimal_places=7, null=True, blank=True
    )
    total_deliveries = models.PositiveIntegerField(default=0)
    total_earnings = models.DecimalField(max_digits=10, decimal_places=2, default=0)
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        db_table = "delivery_partner_profiles"

    def __str__(self):
        return f"Rider: {self.user.phone} ({self.vehicle_type})"


class DeliveryTracking(models.Model):
    order = models.ForeignKey(
        Order, on_delete=models.CASCADE, related_name="tracking_points"
    )
    latitude = models.DecimalField(max_digits=10, decimal_places=7)
    longitude = models.DecimalField(max_digits=10, decimal_places=7)
    timestamp = models.DateTimeField(auto_now_add=True)

    class Meta:
        db_table = "delivery_tracking"
        ordering = ["-timestamp"]
