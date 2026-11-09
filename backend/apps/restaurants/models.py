from django.conf import settings
from django.core.validators import MaxValueValidator, MinValueValidator
from django.db import models


class Restaurant(models.Model):
    class CuisineType(models.TextChoices):
        NORTH_INDIAN = "NORTH_INDIAN", "North Indian"
        SOUTH_INDIAN = "SOUTH_INDIAN", "South Indian"
        CHINESE = "CHINESE", "Chinese"
        FAST_FOOD = "FAST_FOOD", "Fast Food"
        STREET_FOOD = "STREET_FOOD", "Street Food"
        BIRYANI = "BIRYANI", "Biryani"
        PIZZA = "PIZZA", "Pizza"
        DESSERTS = "DESSERTS", "Desserts"
        BEVERAGES = "BEVERAGES", "Beverages"
        THALI = "THALI", "Thali"
        OTHER = "OTHER", "Other"

    owner = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.CASCADE,
        related_name="restaurants",
    )
    name = models.CharField(max_length=200)
    description = models.TextField(blank=True)
    cuisine_type = models.CharField(max_length=20, choices=CuisineType.choices)
    image = models.ImageField(upload_to="restaurants/", blank=True, null=True)
    cover_image = models.ImageField(upload_to="restaurants/covers/", blank=True, null=True)
    phone = models.CharField(max_length=15)
    address = models.CharField(max_length=255)
    city = models.CharField(max_length=100)
    state = models.CharField(max_length=100)
    pincode = models.CharField(max_length=10)
    latitude = models.DecimalField(max_digits=10, decimal_places=7)
    longitude = models.DecimalField(max_digits=10, decimal_places=7)
    rating = models.DecimalField(
        max_digits=2, decimal_places=1, default=0,
        validators=[MinValueValidator(0), MaxValueValidator(5)],
    )
    total_ratings = models.PositiveIntegerField(default=0)
    avg_delivery_time = models.PositiveIntegerField(default=30, help_text="Minutes")
    delivery_radius_km = models.DecimalField(max_digits=4, decimal_places=1, default=5.0)
    min_order_amount = models.DecimalField(max_digits=8, decimal_places=2, default=99)
    delivery_fee = models.DecimalField(max_digits=6, decimal_places=2, default=20)
    is_veg_only = models.BooleanField(default=False)
    is_accepting_orders = models.BooleanField(default=True)
    opening_time = models.TimeField(default="09:00")
    closing_time = models.TimeField(default="22:00")
    is_active = models.BooleanField(default=True)
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        db_table = "restaurants"
        ordering = ["-rating", "name"]

    def __str__(self):
        return self.name


class RestaurantRating(models.Model):
    restaurant = models.ForeignKey(
        Restaurant, on_delete=models.CASCADE, related_name="ratings"
    )
    user = models.ForeignKey(
        settings.AUTH_USER_MODEL, on_delete=models.CASCADE
    )
    rating = models.PositiveSmallIntegerField(
        validators=[MinValueValidator(1), MaxValueValidator(5)]
    )
    review = models.TextField(blank=True)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        db_table = "restaurant_ratings"
        unique_together = ["restaurant", "user"]

    def __str__(self):
        return f"{self.restaurant.name} - {self.rating} by {self.user.phone}"
