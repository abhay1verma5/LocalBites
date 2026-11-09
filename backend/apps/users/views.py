from django.utils import timezone
from rest_framework import generics, status
from rest_framework.permissions import AllowAny, IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView
from rest_framework_simplejwt.tokens import RefreshToken

from .models import Address, OTP, User
from .serializers import (
    AddressSerializer,
    SendOTPSerializer,
    SetRoleSerializer,
    UserProfileSerializer,
    VerifyOTPSerializer,
)


class SendOTPView(APIView):
    permission_classes = [AllowAny]

    def post(self, request):
        serializer = SendOTPSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        phone = serializer.validated_data["phone"]

        # Invalidate old OTPs
        OTP.objects.filter(phone=phone, is_used=False).update(is_used=True)

        otp_code = OTP.generate_otp()
        OTP.objects.create(phone=phone, otp_code=otp_code)

        # TODO: Send OTP via SMS provider (MSG91, Twilio, etc.)
        # For dev, return OTP in response
        return Response(
            {"message": "OTP sent successfully", "otp": otp_code},
            status=status.HTTP_200_OK,
        )


class VerifyOTPView(APIView):
    permission_classes = [AllowAny]

    def post(self, request):
        serializer = VerifyOTPSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        phone = serializer.validated_data["phone"]
        otp_code = serializer.validated_data["otp_code"]

        otp = OTP.objects.filter(
            phone=phone, otp_code=otp_code, is_used=False
        ).first()

        if not otp:
            return Response(
                {"error": "Invalid OTP"}, status=status.HTTP_400_BAD_REQUEST
            )

        if otp.is_expired:
            return Response(
                {"error": "OTP expired"}, status=status.HTTP_400_BAD_REQUEST
            )

        otp.is_used = True
        otp.save()

        user, created = User.objects.get_or_create(
            phone=phone,
            defaults={"username": phone, "is_phone_verified": True},
        )
        if not created:
            user.is_phone_verified = True
            user.save()

        refresh = RefreshToken.for_user(user)
        return Response({
            "access": str(refresh.access_token),
            "refresh": str(refresh),
            "is_new_user": created,
            "has_role": bool(user.role),
            "role": user.role,
        })


class SetRoleView(APIView):
    permission_classes = [IsAuthenticated]

    def post(self, request):
        serializer = SetRoleSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        request.user.role = serializer.validated_data["role"]
        request.user.save()
        return Response({"role": request.user.role})


class UserProfileView(generics.RetrieveUpdateAPIView):
    serializer_class = UserProfileSerializer
    permission_classes = [IsAuthenticated]

    def get_object(self):
        return self.request.user


class AddressListCreateView(generics.ListCreateAPIView):
    serializer_class = AddressSerializer
    permission_classes = [IsAuthenticated]

    def get_queryset(self):
        return Address.objects.filter(user=self.request.user)


class AddressDetailView(generics.RetrieveUpdateDestroyAPIView):
    serializer_class = AddressSerializer
    permission_classes = [IsAuthenticated]

    def get_queryset(self):
        return Address.objects.filter(user=self.request.user)
