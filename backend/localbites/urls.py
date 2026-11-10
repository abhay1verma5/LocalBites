from django.contrib import admin
from django.http import JsonResponse
from django.urls import include, path
from django.conf import settings
from django.conf.urls.static import static


def health_check(request):
    return JsonResponse({"status": "ok"})


urlpatterns = [
    path("", health_check),
    path("health/", health_check),
    path("admin/", admin.site.urls),
    path("api/auth/", include("apps.users.urls")),
    path("api/restaurants/", include("apps.restaurants.urls")),
    path("api/menu/", include("apps.menu.urls")),
    path("api/orders/", include("apps.orders.urls")),
    path("api/payments/", include("apps.payments.urls")),
    path("api/delivery/", include("apps.delivery.urls")),
]

if settings.DEBUG:
    urlpatterns += static(settings.MEDIA_URL, document_root=settings.MEDIA_ROOT)
