from django.urls import path

from . import views

urlpatterns = [
    path("", views.RestaurantListView.as_view(), name="restaurant-list"),
    path("<int:pk>/", views.RestaurantDetailView.as_view(), name="restaurant-detail"),
    path("<int:pk>/ratings/", views.RestaurantRatingListCreateView.as_view(), name="restaurant-ratings"),
    path("owner/", views.OwnerRestaurantListCreateView.as_view(), name="owner-restaurant-list"),
    path("owner/<int:pk>/", views.OwnerRestaurantDetailView.as_view(), name="owner-restaurant-detail"),
    path("owner/<int:pk>/toggle-orders/", views.ToggleAcceptingOrdersView.as_view(), name="toggle-orders"),
]
