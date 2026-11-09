from rest_framework import serializers

from .models import Restaurant, RestaurantRating


class RestaurantListSerializer(serializers.ModelSerializer):
    class Meta:
        model = Restaurant
        fields = [
            "id", "name", "cuisine_type", "image", "rating", "total_ratings",
            "avg_delivery_time", "delivery_fee", "min_order_amount",
            "is_veg_only", "is_accepting_orders", "city",
        ]


class RestaurantDetailSerializer(serializers.ModelSerializer):
    class Meta:
        model = Restaurant
        fields = [
            "id", "name", "description", "cuisine_type", "image", "cover_image",
            "phone", "address", "city", "state", "pincode", "latitude", "longitude",
            "rating", "total_ratings", "avg_delivery_time", "delivery_radius_km",
            "min_order_amount", "delivery_fee", "is_veg_only", "is_accepting_orders",
            "opening_time", "closing_time", "created_at",
        ]


class RestaurantOwnerSerializer(serializers.ModelSerializer):
    class Meta:
        model = Restaurant
        fields = [
            "id", "name", "description", "cuisine_type", "image", "cover_image",
            "phone", "address", "city", "state", "pincode", "latitude", "longitude",
            "delivery_radius_km", "min_order_amount", "delivery_fee", "is_veg_only",
            "is_accepting_orders", "opening_time", "closing_time",
        ]

    def create(self, validated_data):
        validated_data["owner"] = self.context["request"].user
        return super().create(validated_data)


class RestaurantRatingSerializer(serializers.ModelSerializer):
    user_name = serializers.CharField(source="user.first_name", read_only=True)

    class Meta:
        model = RestaurantRating
        fields = ["id", "rating", "review", "user_name", "created_at"]
        read_only_fields = ["id", "created_at"]

    def create(self, validated_data):
        validated_data["user"] = self.context["request"].user
        validated_data["restaurant_id"] = self.context["restaurant_id"]
        return super().create(validated_data)
