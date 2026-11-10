from django.urls import path

from . import views

urlpatterns = [
    path("place/", views.PlaceOrderView.as_view(), name="place-order"),
    path("my/", views.CustomerOrderListView.as_view(), name="customer-orders"),
    path("<int:pk>/", views.OrderDetailView.as_view(), name="order-detail"),
    path("<int:pk>/cancel/", views.CancelOrderView.as_view(), name="cancel-order"),
    # Restaurant owner
    path("restaurant/", views.RestaurantOrderListView.as_view(), name="restaurant-orders"),
    path("<int:pk>/accept/", views.AcceptOrderView.as_view(), name="accept-order"),
    path("<int:pk>/preparing/", views.MarkPreparingView.as_view(), name="mark-preparing"),
    path("<int:pk>/ready/", views.MarkReadyView.as_view(), name="mark-ready"),
]
