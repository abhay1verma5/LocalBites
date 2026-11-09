import hmac
import hashlib

from django.conf import settings
from rest_framework import status
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from apps.orders.models import Order

from .models import Payment
from .serializers import CreatePaymentSerializer, PaymentSerializer, VerifyPaymentSerializer


class CreatePaymentView(APIView):
    permission_classes = [IsAuthenticated]

    def post(self, request):
        serializer = CreatePaymentSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)

        order = Order.objects.filter(
            id=serializer.validated_data["order_id"],
            customer=request.user,
            status=Order.Status.PLACED,
        ).first()
        if not order:
            return Response({"error": "Invalid order"}, status=status.HTTP_400_BAD_REQUEST)

        method = serializer.validated_data["method"]

        if method == Payment.Method.COD:
            payment = Payment.objects.create(
                order=order, method=method, amount=order.total,
                status=Payment.Status.SUCCESS,
            )
            return Response(PaymentSerializer(payment).data)

        # UPI via Razorpay
        if not settings.RAZORPAY_KEY_ID:
            return Response(
                {"error": "Payment gateway not configured"},
                status=status.HTTP_503_SERVICE_UNAVAILABLE,
            )

        import razorpay
        client = razorpay.Client(
            auth=(settings.RAZORPAY_KEY_ID, settings.RAZORPAY_KEY_SECRET)
        )
        rz_order = client.order.create({
            "amount": int(order.total * 100),  # paise
            "currency": "INR",
            "receipt": f"order_{order.id}",
        })

        payment = Payment.objects.create(
            order=order, method=method, amount=order.total,
            razorpay_order_id=rz_order["id"],
        )

        return Response({
            "payment_id": payment.id,
            "razorpay_order_id": rz_order["id"],
            "razorpay_key": settings.RAZORPAY_KEY_ID,
            "amount": int(order.total * 100),
            "currency": "INR",
        })


class VerifyPaymentView(APIView):
    permission_classes = [IsAuthenticated]

    def post(self, request):
        serializer = VerifyPaymentSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        data = serializer.validated_data

        payment = Payment.objects.filter(
            razorpay_order_id=data["razorpay_order_id"],
            order__customer=request.user,
        ).first()
        if not payment:
            return Response({"error": "Payment not found"}, status=status.HTTP_404_NOT_FOUND)

        # Verify signature
        message = f"{data['razorpay_order_id']}|{data['razorpay_payment_id']}"
        expected_signature = hmac.new(
            settings.RAZORPAY_KEY_SECRET.encode(),
            message.encode(),
            hashlib.sha256,
        ).hexdigest()

        if expected_signature == data["razorpay_signature"]:
            payment.razorpay_payment_id = data["razorpay_payment_id"]
            payment.razorpay_signature = data["razorpay_signature"]
            payment.status = Payment.Status.SUCCESS
            payment.save()
            return Response({"status": "success"})
        else:
            payment.status = Payment.Status.FAILED
            payment.save()
            return Response(
                {"error": "Payment verification failed"},
                status=status.HTTP_400_BAD_REQUEST,
            )
