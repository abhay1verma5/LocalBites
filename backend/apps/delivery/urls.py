from django.urls import path

from . import views

urlpatterns = [
    path("profile/", views.DeliveryProfileView.as_view(), name="delivery-profile"),
    path("toggle-available/", views.ToggleAvailabilityView.as_view(), name="toggle-available"),
    path("update-location/", views.UpdateLocationView.as_view(), name="update-location"),
    path("available-orders/", views.AvailableOrdersView.as_view(), name="available-orders"),
    path("orders/<int:pk>/accept/", views.AcceptDeliveryView.as_view(), name="accept-delivery"),
    path("orders/<int:pk>/delivered/", views.ConfirmDeliveryView.as_view(), name="confirm-delivery"),
    path("history/", views.DeliveryHistoryView.as_view(), name="delivery-history"),
    path("earnings/", views.DeliveryEarningsView.as_view(), name="delivery-earnings"),
]
