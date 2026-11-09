from rest_framework import serializers

from .models import DeliveryPartnerProfile, DeliveryTracking


class DeliveryPartnerProfileSerializer(serializers.ModelSerializer):
    phone = serializers.CharField(source="user.phone", read_only=True)
    name = serializers.CharField(source="user.first_name", read_only=True)

    class Meta:
        model = DeliveryPartnerProfile
        fields = [
            "id", "phone", "name", "vehicle_type", "vehicle_number",
            "license_number", "is_available", "is_verified",
            "total_deliveries", "total_earnings",
        ]
        read_only_fields = ["id", "is_verified", "total_deliveries", "total_earnings"]


class UpdateLocationSerializer(serializers.Serializer):
    latitude = serializers.DecimalField(max_digits=10, decimal_places=7)
    longitude = serializers.DecimalField(max_digits=10, decimal_places=7)


class DeliveryTrackingSerializer(serializers.ModelSerializer):
    class Meta:
        model = DeliveryTracking
        fields = ["latitude", "longitude", "timestamp"]
