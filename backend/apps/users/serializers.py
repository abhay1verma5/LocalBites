from rest_framework import serializers

from .models import Address, OTP, User


class SendOTPSerializer(serializers.Serializer):
    phone = serializers.CharField(max_length=15)


class VerifyOTPSerializer(serializers.Serializer):
    phone = serializers.CharField(max_length=15)
    otp_code = serializers.CharField(max_length=6)


class SetRoleSerializer(serializers.Serializer):
    role = serializers.ChoiceField(choices=User.Role.choices)


class UserProfileSerializer(serializers.ModelSerializer):
    class Meta:
        model = User
        fields = ["id", "phone", "username", "first_name", "last_name", "email",
                  "role", "avatar", "is_phone_verified", "created_at"]
        read_only_fields = ["id", "phone", "is_phone_verified", "created_at"]


class AddressSerializer(serializers.ModelSerializer):
    class Meta:
        model = Address
        fields = ["id", "label", "address_line", "landmark", "city", "state",
                  "pincode", "latitude", "longitude", "is_default", "created_at"]
        read_only_fields = ["id", "created_at"]

    def create(self, validated_data):
        validated_data["user"] = self.context["request"].user
        if validated_data.get("is_default"):
            Address.objects.filter(user=validated_data["user"]).update(is_default=False)
        return super().create(validated_data)
