import json

from channels.db import database_sync_to_async
from channels.generic.websocket import AsyncWebsocketConsumer

from .models import Order


class OrderTrackingConsumer(AsyncWebsocketConsumer):
    async def connect(self):
        self.order_id = self.scope["url_route"]["kwargs"]["order_id"]
        self.room_group_name = f"order_{self.order_id}"

        await self.channel_layer.group_add(
            self.room_group_name, self.channel_name
        )
        await self.accept()

        # Send current order status
        order_data = await self.get_order_status()
        if order_data:
            await self.send(text_data=json.dumps(order_data))

    async def disconnect(self, close_code):
        await self.channel_layer.group_discard(
            self.room_group_name, self.channel_name
        )

    async def order_status_update(self, event):
        await self.send(text_data=json.dumps(event["data"]))

    async def delivery_location_update(self, event):
        await self.send(text_data=json.dumps({
            "type": "location",
            "latitude": event["latitude"],
            "longitude": event["longitude"],
        }))

    @database_sync_to_async
    def get_order_status(self):
        try:
            order = Order.objects.select_related(
                "restaurant", "delivery_partner"
            ).get(id=self.order_id)
            return {
                "type": "status",
                "status": order.status,
                "restaurant_name": order.restaurant.name,
                "delivery_partner": (
                    order.delivery_partner.first_name
                    if order.delivery_partner else None
                ),
            }
        except Order.DoesNotExist:
            return None
