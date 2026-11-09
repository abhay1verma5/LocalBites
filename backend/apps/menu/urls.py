from django.urls import path

from . import views

urlpatterns = [
    # Customer: view restaurant menu
    path("restaurant/<int:restaurant_id>/", views.RestaurantMenuView.as_view(), name="restaurant-menu"),
    # Owner: manage categories
    path("owner/<int:restaurant_id>/categories/", views.OwnerMenuCategoryListCreateView.as_view(), name="owner-categories"),
    path("owner/categories/<int:pk>/", views.OwnerMenuCategoryDetailView.as_view(), name="owner-category-detail"),
    # Owner: manage items
    path("owner/<int:restaurant_id>/items/", views.OwnerMenuItemListCreateView.as_view(), name="owner-items"),
    path("owner/items/<int:pk>/", views.OwnerMenuItemDetailView.as_view(), name="owner-item-detail"),
    path("owner/items/<int:pk>/toggle/", views.ToggleMenuItemAvailabilityView.as_view(), name="toggle-item"),
]
