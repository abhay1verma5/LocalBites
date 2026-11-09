# LocalBites

Swiggy-type food ordering app for Tier 3/4 cities in India.

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Android | Kotlin + Jetpack Compose + Material 3 |
| Architecture | MVVM + Clean Architecture (multi-module) |
| DI | Hilt |
| Networking | Retrofit + OkHttp + Moshi |
| Backend | Django + Django REST Framework |
| Database | PostgreSQL |
| Real-time | Django Channels + Redis (WebSocket) |
| Payments | Razorpay SDK (UPI + COD) |

## Project Structure

```
LocalBites/
├── android/          # Android app (Kotlin/Compose)
│   ├── app/          # Main module
│   ├── core/         # Shared modules (ui, network, database, common)
│   └── feature/      # Feature modules (auth, customer, restaurant, delivery)
├── backend/          # Django backend
│   ├── localbites/   # Project config
│   └── apps/         # Django apps (users, restaurants, menu, orders, etc.)
└── README.md
```

## User Types

1. **Customer** - Browse restaurants, order food, track delivery
2. **Restaurant Owner** - Manage menu, accept orders, view earnings
3. **Delivery Partner** - Accept deliveries, navigate, earn

## Setup

### Backend
```bash
cd backend
docker-compose up
```

### Android
Open `android/` in Android Studio and run on emulator/device.
