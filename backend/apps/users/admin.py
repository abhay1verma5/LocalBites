from django.contrib import admin
from django.contrib.auth.admin import UserAdmin as BaseUserAdmin

from .models import Address, OTP, User


@admin.register(User)
class UserAdmin(BaseUserAdmin):
    list_display = ["phone", "username", "role", "is_phone_verified", "date_joined"]
    list_filter = ["role", "is_phone_verified"]
    search_fields = ["phone", "username", "first_name", "last_name"]
    fieldsets = BaseUserAdmin.fieldsets + (
        ("LocalBites", {"fields": ("phone", "role", "avatar", "is_phone_verified")}),
    )


@admin.register(OTP)
class OTPAdmin(admin.ModelAdmin):
    list_display = ["phone", "otp_code", "is_used", "created_at", "expires_at"]
    list_filter = ["is_used"]


@admin.register(Address)
class AddressAdmin(admin.ModelAdmin):
    list_display = ["user", "label", "city", "pincode", "is_default"]
    list_filter = ["label", "city"]
