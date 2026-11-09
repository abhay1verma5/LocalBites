from django.db.models import F
from rest_framework import generics, status
from rest_framework.permissions import AllowAny, IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from .models import Restaurant, RestaurantRating
from .serializers import (
    RestaurantDetailSerializer,
    RestaurantListSerializer,
    RestaurantOwnerSerializer,
    RestaurantRatingSerializer,
)


class RestaurantListView(generics.ListAPIView):
    """List nearby restaurants for customers."""
    serializer_class = RestaurantListSerializer
    permission_classes = [AllowAny]
    filterset_fields = ["cuisine_type", "is_veg_only", "city"]
    search_fields = ["name", "cuisine_type"]
    ordering_fields = ["rating", "avg_delivery_time", "delivery_fee"]

    def get_queryset(self):
        qs = Restaurant.objects.filter(is_active=True)
        city = self.request.query_params.get("city")
        if city:
            qs = qs.filter(city__icontains=city)
        return qs


class RestaurantDetailView(generics.RetrieveAPIView):
    """Restaurant detail for customers."""
    serializer_class = RestaurantDetailSerializer
    permission_classes = [AllowAny]
    queryset = Restaurant.objects.filter(is_active=True)


class OwnerRestaurantListCreateView(generics.ListCreateAPIView):
    """Owner's restaurant management."""
    serializer_class = RestaurantOwnerSerializer
    permission_classes = [IsAuthenticated]

    def get_queryset(self):
        return Restaurant.objects.filter(owner=self.request.user)


class OwnerRestaurantDetailView(generics.RetrieveUpdateAPIView):
    """Owner's restaurant detail/update."""
    serializer_class = RestaurantOwnerSerializer
    permission_classes = [IsAuthenticated]

    def get_queryset(self):
        return Restaurant.objects.filter(owner=self.request.user)


class ToggleAcceptingOrdersView(APIView):
    permission_classes = [IsAuthenticated]

    def post(self, request, pk):
        restaurant = Restaurant.objects.filter(owner=request.user, pk=pk).first()
        if not restaurant:
            return Response({"error": "Not found"}, status=status.HTTP_404_NOT_FOUND)
        restaurant.is_accepting_orders = not restaurant.is_accepting_orders
        restaurant.save()
        return Response({"is_accepting_orders": restaurant.is_accepting_orders})


class RestaurantRatingListCreateView(generics.ListCreateAPIView):
    serializer_class = RestaurantRatingSerializer
    permission_classes = [IsAuthenticated]

    def get_queryset(self):
        return RestaurantRating.objects.filter(restaurant_id=self.kwargs["pk"])

    def get_serializer_context(self):
        ctx = super().get_serializer_context()
        ctx["restaurant_id"] = self.kwargs["pk"]
        return ctx

    def perform_create(self, serializer):
        instance = serializer.save()
        restaurant = instance.restaurant
        restaurant.total_ratings = F("total_ratings") + 1
        restaurant.save()
        restaurant.refresh_from_db()
        avg = RestaurantRating.objects.filter(restaurant=restaurant).values_list(
            "rating", flat=True
        )
        restaurant.rating = sum(avg) / len(avg)
        restaurant.save()
