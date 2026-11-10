from decimal import Decimal

from asgiref.sync import async_to_sync
from channels.layers import get_channel_layer
from django.utils import timezone
from rest_framework import generics, status
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from apps.orders.models import Order
from apps.orders.serializers import OrderListSerializer

from .models import DeliveryPartnerProfile, DeliveryTracking
from .serializers import (
    DeliveryPartnerProfileSerializer,
    UpdateLocationSerializer,
)


class DeliveryProfileView(generics.RetrieveUpdateAPIView):
    serializer_class = DeliveryPartnerProfileSerializer
    permission_classes = [IsAuthenticated]

    def get_object(self):
        profile, _ = DeliveryPartnerProfile.objects.get_or_create(
            user=self.request.user,
            defaults={"vehicle_type": DeliveryPartnerProfile.VehicleType.MOTORCYCLE},
        )
        return profile


class ToggleAvailabilityView(APIView):
    permission_classes = [IsAuthenticated]

    def post(self, request):
        profile = DeliveryPartnerProfile.objects.filter(user=request.user).first()
        if not profile:
            return Response({"error": "Profile not found"}, status=status.HTTP_404_NOT_FOUND)
        profile.is_available = not profile.is_available
        profile.save()
        return Response({"is_available": profile.is_available})


class UpdateLocationView(APIView):
    permission_classes = [IsAuthenticated]

    def post(self, request):
        serializer = UpdateLocationSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)

        profile = DeliveryPartnerProfile.objects.filter(user=request.user).first()
        if not profile:
            return Response({"error": "Profile not found"}, status=status.HTTP_404_NOT_FOUND)

        profile.current_latitude = serializer.validated_data["latitude"]
        profile.current_longitude = serializer.validated_data["longitude"]
        profile.save()

        # Also update tracking for active order
        active_order = Order.objects.filter(
            delivery_partner=request.user,
            status__in=[Order.Status.PICKED_UP, Order.Status.ON_THE_WAY],
        ).first()

        if active_order:
            DeliveryTracking.objects.create(
                order=active_order,
                latitude=serializer.validated_data["latitude"],
                longitude=serializer.validated_data["longitude"],
            )
            # Send via WebSocket (graceful if Redis unavailable)
            try:
                channel_layer = get_channel_layer()
                async_to_sync(channel_layer.group_send)(
                    f"order_{active_order.id}",
                    {
                        "type": "delivery_location_update",
                        "latitude": str(serializer.validated_data["latitude"]),
                        "longitude": str(serializer.validated_data["longitude"]),
                    },
                )
            except Exception:
                pass

        return Response({"status": "location updated"})


class AvailableOrdersView(generics.ListAPIView):
    """Orders ready for pickup near the delivery partner."""
    serializer_class = OrderListSerializer
    permission_classes = [IsAuthenticated]

    def get_queryset(self):
        return Order.objects.filter(
            status=Order.Status.READY,
            delivery_partner__isnull=True,
        ).select_related("restaurant").prefetch_related("items")


class AcceptDeliveryView(APIView):
    permission_classes = [IsAuthenticated]

    def post(self, request, pk):
        order = Order.objects.filter(
            pk=pk, status=Order.Status.READY, delivery_partner__isnull=True
        ).first()
        if not order:
            return Response({"error": "Order not available"}, status=status.HTTP_400_BAD_REQUEST)

        order.delivery_partner = request.user
        order.status = Order.Status.PICKED_UP
        order.picked_up_at = timezone.now()
        order.save()

        # Notify via WebSocket (graceful if Redis unavailable)
        try:
            channel_layer = get_channel_layer()
            async_to_sync(channel_layer.group_send)(
                f"order_{order.id}",
                {
                    "type": "order_status_update",
                    "data": {
                        "type": "status",
                        "status": order.status,
                        "delivery_partner": request.user.first_name or request.user.phone,
                    },
                },
            )
        except Exception:
            pass

        return Response({"status": "Delivery accepted"})


class ConfirmDeliveryView(APIView):
    permission_classes = [IsAuthenticated]

    def post(self, request, pk):
        order = Order.objects.filter(
            pk=pk, delivery_partner=request.user,
            status__in=[Order.Status.PICKED_UP, Order.Status.ON_THE_WAY],
        ).first()
        if not order:
            return Response({"error": "Order not found"}, status=status.HTTP_400_BAD_REQUEST)

        order.status = Order.Status.DELIVERED
        order.delivered_at = timezone.now()
        order.save()

        # Update partner stats
        profile = DeliveryPartnerProfile.objects.get(user=request.user)
        profile.total_deliveries += 1
        profile.total_earnings += order.delivery_fee
        profile.save()

        # Notify via WebSocket (graceful if Redis unavailable)
        try:
            channel_layer = get_channel_layer()
            async_to_sync(channel_layer.group_send)(
                f"order_{order.id}",
                {
                    "type": "order_status_update",
                    "data": {"type": "status", "status": order.status},
                },
            )
        except Exception:
            pass

        return Response({"status": "Delivered successfully"})


class DeliveryHistoryView(generics.ListAPIView):
    serializer_class = OrderListSerializer
    permission_classes = [IsAuthenticated]

    def get_queryset(self):
        return Order.objects.filter(
            delivery_partner=self.request.user,
            status=Order.Status.DELIVERED,
        ).select_related("restaurant").prefetch_related("items")


class DeliveryEarningsView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request):
        profile = DeliveryPartnerProfile.objects.filter(user=request.user).first()
        if not profile:
            return Response({"error": "Profile not found"}, status=status.HTTP_404_NOT_FOUND)

        today = timezone.now().date()
        today_orders = Order.objects.filter(
            delivery_partner=request.user,
            status=Order.Status.DELIVERED,
            delivered_at__date=today,
        )
        today_earnings = sum(o.delivery_fee for o in today_orders)

        return Response({
            "total_deliveries": profile.total_deliveries,
            "total_earnings": str(profile.total_earnings),
            "today_deliveries": today_orders.count(),
            "today_earnings": str(today_earnings),
        })
