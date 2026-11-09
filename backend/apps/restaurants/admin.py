from django.contrib import admin

from .models import Restaurant, RestaurantRating


@admin.register(Restaurant)
class RestaurantAdmin(admin.ModelAdmin):
    list_display = ["name", "owner", "cuisine_type", "city", "rating", "is_accepting_orders", "is_active"]
    list_filter = ["cuisine_type", "is_veg_only", "is_accepting_orders", "city"]
    search_fields = ["name", "city"]


@admin.register(RestaurantRating)
class RestaurantRatingAdmin(admin.ModelAdmin):
    list_display = ["restaurant", "user", "rating", "created_at"]
