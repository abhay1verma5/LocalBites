from rest_framework import serializers

from .models import Order, OrderItem


class OrderItemSerializer(serializers.ModelSerializer):
    item_total = serializers.ReadOnlyField()

    class Meta:
        model = OrderItem
        fields = ["id", "menu_item", "name", "price", "quantity", "item_total"]


class OrderItemCreateSerializer(serializers.Serializer):
    menu_item_id = serializers.IntegerField()
    quantity = serializers.IntegerField(min_value=1)


class OrderCreateSerializer(serializers.Serializer):
    restaurant_id = serializers.IntegerField()
    delivery_address_id = serializers.IntegerField()
    items = OrderItemCreateSerializer(many=True)
    special_instructions = serializers.CharField(required=False, default="")


class OrderListSerializer(serializers.ModelSerializer):
    items = OrderItemSerializer(many=True, read_only=True)
    restaurant_name = serializers.CharField(source="restaurant.name", read_only=True)
    restaurant_image = serializers.ImageField(source="restaurant.image", read_only=True)
    delivery_partner_name = serializers.SerializerMethodField()

    class Meta:
        model = Order
        fields = [
            "id", "restaurant_name", "restaurant_image", "status",
            "subtotal", "delivery_fee", "tax", "discount", "total",
            "special_instructions", "placed_at", "delivered_at",
            "items", "delivery_partner_name",
        ]

    def get_delivery_partner_name(self, obj):
        if obj.delivery_partner:
            return obj.delivery_partner.first_name or obj.delivery_partner.phone
        return None


class OrderDetailSerializer(serializers.ModelSerializer):
    items = OrderItemSerializer(many=True, read_only=True)
    restaurant_name = serializers.CharField(source="restaurant.name", read_only=True)
    restaurant_phone = serializers.CharField(source="restaurant.phone", read_only=True)
    customer_phone = serializers.CharField(source="customer.phone", read_only=True)
    customer_name = serializers.SerializerMethodField()
    delivery_partner_name = serializers.SerializerMethodField()
    delivery_partner_phone = serializers.SerializerMethodField()

    class Meta:
        model = Order
        fields = [
            "id", "status", "restaurant_name", "restaurant_phone",
            "customer_phone", "customer_name",
            "delivery_partner_name", "delivery_partner_phone",
            "subtotal", "delivery_fee", "tax", "discount", "total",
            "special_instructions", "cancelled_reason",
            "placed_at", "confirmed_at", "prepared_at",
            "picked_up_at", "delivered_at", "cancelled_at",
            "items",
        ]

    def get_customer_name(self, obj):
        return obj.customer.first_name or obj.customer.phone

    def get_delivery_partner_name(self, obj):
        if obj.delivery_partner:
            return obj.delivery_partner.first_name or obj.delivery_partner.phone
        return None

    def get_delivery_partner_phone(self, obj):
        if obj.delivery_partner:
            return obj.delivery_partner.phone
        return None
