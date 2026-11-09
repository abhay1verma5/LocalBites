from django.urls import path

from . import views

urlpatterns = [
    path("create/", views.CreatePaymentView.as_view(), name="create-payment"),
    path("verify/", views.VerifyPaymentView.as_view(), name="verify-payment"),
]
