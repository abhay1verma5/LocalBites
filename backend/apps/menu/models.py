from django.db import models

from apps.restaurants.models import Restaurant


class MenuCategory(models.Model):
    restaurant = models.ForeignKey(
        Restaurant, on_delete=models.CASCADE, related_name="menu_categories"
    )
    name = models.CharField(max_length=100)
    sort_order = models.PositiveIntegerField(default=0)
    is_active = models.BooleanField(default=True)

    class Meta:
        db_table = "menu_categories"
        verbose_name_plural = "Menu Categories"
        ordering = ["sort_order", "name"]
        unique_together = ["restaurant", "name"]

    def __str__(self):
        return f"{self.restaurant.name} - {self.name}"


class MenuItem(models.Model):
    class FoodType(models.TextChoices):
        VEG = "VEG", "Vegetarian"
        NON_VEG = "NON_VEG", "Non-Vegetarian"
        EGG = "EGG", "Egg"

    category = models.ForeignKey(
        MenuCategory, on_delete=models.CASCADE, related_name="items"
    )
    name = models.CharField(max_length=200)
    description = models.TextField(blank=True)
    price = models.DecimalField(max_digits=8, decimal_places=2)
    discounted_price = models.DecimalField(
        max_digits=8, decimal_places=2, blank=True, null=True
    )
    image = models.ImageField(upload_to="menu_items/", blank=True, null=True)
    food_type = models.CharField(max_length=10, choices=FoodType.choices)
    is_available = models.BooleanField(default=True)
    is_bestseller = models.BooleanField(default=False)
    preparation_time = models.PositiveIntegerField(default=15, help_text="Minutes")
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        db_table = "menu_items"
        ordering = ["-is_bestseller", "name"]

    def __str__(self):
        return f"{self.name} - Rs.{self.effective_price}"

    @property
    def effective_price(self):
        return self.discounted_price if self.discounted_price else self.price
