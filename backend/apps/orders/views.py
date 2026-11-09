from decimal import Decimal

from django.db.models import Q
from django.utils import timezone
from rest_framework import generics, status
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from apps.menu.models import MenuItem
from apps.restaurants.models import Restaurant
from apps.users.models import Address

from .models import Order, OrderItem
from .serializers import (
    OrderCreateSerializer,
    OrderDetailSerializer,
    OrderListSerializer,
)


class PlaceOrderView(APIView):
    permission_classes = [IsAuthenticated]

    def post(self, request):
        serializer = OrderCreateSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        data = serializer.validated_data

        restaurant = Restaurant.objects.filter(
            id=data["restaurant_id"], is_active=True, is_accepting_orders=True
        ).first()
        if not restaurant:
            return Response(
                {"error": "Restaurant not available"},
                status=status.HTTP_400_BAD_REQUEST,
            )

        address = Address.objects.filter(
            id=data["delivery_address_id"], user=request.user
        ).first()
        if not address:
            return Response(
                {"error": "Invalid delivery address"},
                status=status.HTTP_400_BAD_REQUEST,
            )

        # Calculate totals
        subtotal = Decimal("0")
        order_items = []
        for item_data in data["items"]:
            menu_item = MenuItem.objects.filter(
                id=item_data["menu_item_id"], is_available=True
            ).first()
            if not menu_item:
                return Response(
                    {"error": f"Menu item {item_data['menu_item_id']} not available"},
                    status=status.HTTP_400_BAD_REQUEST,
                )
            price = menu_item.effective_price
            quantity = item_data["quantity"]
            subtotal += price * quantity
            order_items.append({
                "menu_item": menu_item,
                "name": menu_item.name,
                "price": price,
                "quantity": quantity,
            })

        if subtotal < restaurant.min_order_amount:
            return Response(
                {"error": f"Minimum order amount is Rs.{restaurant.min_order_amount}"},
                status=status.HTTP_400_BAD_REQUEST,
            )

        delivery_fee = restaurant.delivery_fee
        tax = subtotal * Decimal("0.05")  # 5% GST
        total = subtotal + delivery_fee + tax

        order = Order.objects.create(
            customer=request.user,
            restaurant=restaurant,
            delivery_address=address,
            subtotal=subtotal,
            delivery_fee=delivery_fee,
            tax=tax.quantize(Decimal("0.01")),
            total=total.quantize(Decimal("0.01")),
            special_instructions=data.get("special_instructions", ""),
        )

        for item in order_items:
            OrderItem.objects.create(order=order, **item)

        return Response(
            OrderDetailSerializer(order).data,
            status=status.HTTP_201_CREATED,
        )


class CustomerOrderListView(generics.ListAPIView):
    serializer_class = OrderListSerializer
    permission_classes = [IsAuthenticated]

    def get_queryset(self):
        return Order.objects.filter(
            customer=self.request.user
        ).select_related("restaurant").prefetch_related("items")


class OrderDetailView(generics.RetrieveAPIView):
    serializer_class = OrderDetailSerializer
    permission_classes = [IsAuthenticated]

    def get_queryset(self):
        user = self.request.user
        return Order.objects.filter(
            Q(customer=user)
            | Q(restaurant__owner=user)
            | Q(delivery_partner=user)
        ).select_related("restaurant", "customer", "delivery_partner")


class RestaurantOrderListView(generics.ListAPIView):
    """Restaurant owner: incoming orders."""
    serializer_class = OrderListSerializer
    permission_classes = [IsAuthenticated]

    def get_queryset(self):
        qs = Order.objects.filter(
            restaurant__owner=self.request.user
        ).select_related("restaurant").prefetch_related("items")
        status_filter = self.request.query_params.get("status")
        if status_filter:
            qs = qs.filter(status=status_filter)
        return qs


class AcceptOrderView(APIView):
    permission_classes = [IsAuthenticated]

    def post(self, request, pk):
        order = Order.objects.filter(
            pk=pk, restaurant__owner=request.user, status=Order.Status.PLACED
        ).first()
        if not order:
            return Response({"error": "Order not found or already processed"},
                          status=status.HTTP_400_BAD_REQUEST)
        order.status = Order.Status.CONFIRMED
        order.confirmed_at = timezone.now()
        order.save()
        return Response(OrderDetailSerializer(order).data)


class MarkReadyView(APIView):
    permission_classes = [IsAuthenticated]

    def post(self, request, pk):
        order = Order.objects.filter(
            pk=pk, restaurant__owner=request.user,
            status__in=[Order.Status.CONFIRMED, Order.Status.PREPARING],
        ).first()
        if not order:
            return Response({"error": "Order not found"},
                          status=status.HTTP_400_BAD_REQUEST)
        order.status = Order.Status.READY
        order.prepared_at = timezone.now()
        order.save()
        return Response(OrderDetailSerializer(order).data)


class CancelOrderView(APIView):
    permission_classes = [IsAuthenticated]

    def post(self, request, pk):
        order = Order.objects.filter(
            pk=pk, customer=request.user,
            status__in=[Order.Status.PLACED, Order.Status.CONFIRMED],
        ).first()
        if not order:
            return Response({"error": "Cannot cancel this order"},
                          status=status.HTTP_400_BAD_REQUEST)
        order.status = Order.Status.CANCELLED
        order.cancelled_at = timezone.now()
        order.cancelled_reason = request.data.get("reason", "Cancelled by customer")
        order.save()
        return Response(OrderDetailSerializer(order).data)
