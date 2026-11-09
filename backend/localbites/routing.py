from django.urls import path

from apps.orders.consumers import OrderTrackingConsumer

websocket_urlpatterns = [
    path("ws/order/<int:order_id>/", OrderTrackingConsumer.as_asgi()),
]
