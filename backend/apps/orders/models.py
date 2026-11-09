from django.conf import settings
from django.db import models

from apps.menu.models import MenuItem
from apps.restaurants.models import Restaurant
from apps.users.models import Address


class Order(models.Model):
    class Status(models.TextChoices):
        PLACED = "PLACED", "Order Placed"
        CONFIRMED = "CONFIRMED", "Confirmed by Restaurant"
        PREPARING = "PREPARING", "Being Prepared"
        READY = "READY", "Ready for Pickup"
        PICKED_UP = "PICKED_UP", "Picked Up by Rider"
        ON_THE_WAY = "ON_THE_WAY", "On the Way"
        DELIVERED = "DELIVERED", "Delivered"
        CANCELLED = "CANCELLED", "Cancelled"

    customer = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.CASCADE,
        related_name="customer_orders",
    )
    restaurant = models.ForeignKey(
        Restaurant, on_delete=models.CASCADE, related_name="orders"
    )
    delivery_partner = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.SET_NULL,
        null=True, blank=True,
        related_name="delivery_orders",
    )
    delivery_address = models.ForeignKey(
        Address, on_delete=models.SET_NULL, null=True
    )
    status = models.CharField(
        max_length=20, choices=Status.choices, default=Status.PLACED
    )
    subtotal = models.DecimalField(max_digits=10, decimal_places=2)
    delivery_fee = models.DecimalField(max_digits=6, decimal_places=2)
    tax = models.DecimalField(max_digits=8, decimal_places=2, default=0)
    discount = models.DecimalField(max_digits=8, decimal_places=2, default=0)
    total = models.DecimalField(max_digits=10, decimal_places=2)
    special_instructions = models.TextField(blank=True)
    cancelled_reason = models.TextField(blank=True)
    placed_at = models.DateTimeField(auto_now_add=True)
    confirmed_at = models.DateTimeField(null=True, blank=True)
    prepared_at = models.DateTimeField(null=True, blank=True)
    picked_up_at = models.DateTimeField(null=True, blank=True)
    delivered_at = models.DateTimeField(null=True, blank=True)
    cancelled_at = models.DateTimeField(null=True, blank=True)

    class Meta:
        db_table = "orders"
        ordering = ["-placed_at"]

    def __str__(self):
        return f"Order #{self.id} - {self.customer.phone} from {self.restaurant.name}"


class OrderItem(models.Model):
    order = models.ForeignKey(Order, on_delete=models.CASCADE, related_name="items")
    menu_item = models.ForeignKey(
        MenuItem, on_delete=models.SET_NULL, null=True
    )
    name = models.CharField(max_length=200)
    price = models.DecimalField(max_digits=8, decimal_places=2)
    quantity = models.PositiveIntegerField(default=1)

    class Meta:
        db_table = "order_items"

    def __str__(self):
        return f"{self.quantity}x {self.name}"

    @property
    def item_total(self):
        return self.price * self.quantity
