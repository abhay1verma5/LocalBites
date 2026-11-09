from django.contrib import admin

from .models import Order, OrderItem


class OrderItemInline(admin.TabularInline):
    model = OrderItem
    extra = 0
    readonly_fields = ["name", "price", "quantity"]


@admin.register(Order)
class OrderAdmin(admin.ModelAdmin):
    list_display = ["id", "customer", "restaurant", "status", "total", "placed_at"]
    list_filter = ["status", "placed_at"]
    search_fields = ["customer__phone", "restaurant__name"]
    inlines = [OrderItemInline]
