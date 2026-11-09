from rest_framework import serializers

from .models import MenuCategory, MenuItem


class MenuItemSerializer(serializers.ModelSerializer):
    effective_price = serializers.ReadOnlyField()

    class Meta:
        model = MenuItem
        fields = [
            "id", "name", "description", "price", "discounted_price",
            "effective_price", "image", "food_type", "is_available",
            "is_bestseller", "preparation_time",
        ]


class MenuCategorySerializer(serializers.ModelSerializer):
    items = MenuItemSerializer(many=True, read_only=True)

    class Meta:
        model = MenuCategory
        fields = ["id", "name", "sort_order", "is_active", "items"]


class MenuCategoryCreateSerializer(serializers.ModelSerializer):
    class Meta:
        model = MenuCategory
        fields = ["id", "name", "sort_order", "is_active"]

    def create(self, validated_data):
        validated_data["restaurant_id"] = self.context["restaurant_id"]
        return super().create(validated_data)


class MenuItemCreateSerializer(serializers.ModelSerializer):
    class Meta:
        model = MenuItem
        fields = [
            "id", "category", "name", "description", "price", "discounted_price",
            "image", "food_type", "is_available", "is_bestseller", "preparation_time",
        ]
