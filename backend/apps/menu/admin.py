from django.contrib import admin

from .models import MenuCategory, MenuItem


@admin.register(MenuCategory)
class MenuCategoryAdmin(admin.ModelAdmin):
    list_display = ["name", "restaurant", "sort_order", "is_active"]
    list_filter = ["restaurant", "is_active"]


@admin.register(MenuItem)
class MenuItemAdmin(admin.ModelAdmin):
    list_display = ["name", "category", "price", "food_type", "is_available", "is_bestseller"]
    list_filter = ["food_type", "is_available", "is_bestseller"]
    search_fields = ["name"]
