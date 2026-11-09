from rest_framework import generics
from rest_framework.permissions import AllowAny, IsAuthenticated

from apps.restaurants.models import Restaurant

from .models import MenuCategory, MenuItem
from .serializers import (
    MenuCategoryCreateSerializer,
    MenuCategorySerializer,
    MenuItemCreateSerializer,
    MenuItemSerializer,
)


class RestaurantMenuView(generics.ListAPIView):
    """Full menu for a restaurant (customer view)."""
    serializer_class = MenuCategorySerializer
    permission_classes = [AllowAny]

    def get_queryset(self):
        return MenuCategory.objects.filter(
            restaurant_id=self.kwargs["restaurant_id"],
            is_active=True,
        ).prefetch_related("items")


class OwnerMenuCategoryListCreateView(generics.ListCreateAPIView):
    """Owner: manage menu categories."""
    permission_classes = [IsAuthenticated]

    def get_serializer_class(self):
        if self.request.method == "POST":
            return MenuCategoryCreateSerializer
        return MenuCategorySerializer

    def get_queryset(self):
        return MenuCategory.objects.filter(
            restaurant_id=self.kwargs["restaurant_id"],
            restaurant__owner=self.request.user,
        ).prefetch_related("items")

    def get_serializer_context(self):
        ctx = super().get_serializer_context()
        ctx["restaurant_id"] = self.kwargs["restaurant_id"]
        return ctx


class OwnerMenuCategoryDetailView(generics.RetrieveUpdateDestroyAPIView):
    serializer_class = MenuCategoryCreateSerializer
    permission_classes = [IsAuthenticated]

    def get_queryset(self):
        return MenuCategory.objects.filter(
            restaurant__owner=self.request.user,
        )


class OwnerMenuItemListCreateView(generics.ListCreateAPIView):
    """Owner: manage menu items."""
    permission_classes = [IsAuthenticated]

    def get_serializer_class(self):
        if self.request.method == "POST":
            return MenuItemCreateSerializer
        return MenuItemSerializer

    def get_queryset(self):
        return MenuItem.objects.filter(
            category__restaurant_id=self.kwargs["restaurant_id"],
            category__restaurant__owner=self.request.user,
        )


class OwnerMenuItemDetailView(generics.RetrieveUpdateDestroyAPIView):
    serializer_class = MenuItemCreateSerializer
    permission_classes = [IsAuthenticated]

    def get_queryset(self):
        return MenuItem.objects.filter(
            category__restaurant__owner=self.request.user,
        )


class ToggleMenuItemAvailabilityView(generics.UpdateAPIView):
    serializer_class = MenuItemSerializer
    permission_classes = [IsAuthenticated]

    def get_queryset(self):
        return MenuItem.objects.filter(
            category__restaurant__owner=self.request.user,
        )

    def perform_update(self, serializer):
        item = self.get_object()
        item.is_available = not item.is_available
        item.save()
