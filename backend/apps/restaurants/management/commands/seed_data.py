from django.core.management.base import BaseCommand

from apps.menu.models import MenuCategory, MenuItem
from apps.restaurants.models import Restaurant
from apps.users.models import User


class Command(BaseCommand):
    help = "Seed dummy restaurants and menu items for Haridwar/Basti"

    def handle(self, *args, **options):
        # Create a restaurant owner user if needed
        owner, _ = User.objects.get_or_create(
            phone="+910000000001",
            defaults={
                "username": "restaurant_owner",
                "role": "RESTAURANT_OWNER",
                "is_phone_verified": True,
            },
        )

        if Restaurant.objects.filter(owner=owner).count() >= 5:
            self.stdout.write(self.style.WARNING("Seed data already exists, skipping."))
            return
        # Clean partial seed data
        Restaurant.objects.filter(owner=owner).delete()

        restaurants_data = [
            {
                "name": "Baba Dhaba",
                "description": "Authentic North Indian street food and thalis since 1995",
                "cuisine_type": "NORTH_INDIAN",
                "phone": "+911234500001",
                "address": "Main Market, Harraiya",
                "city": "Harraiya",
                "state": "Uttar Pradesh",
                "pincode": "272155",
                "latitude": 26.7680,
                "longitude": 82.5380,
                "rating": 4.3,
                "total_ratings": 245,
                "avg_delivery_time": 25,
                "delivery_fee": 15,
                "min_order_amount": 99,
                "is_veg_only": True,
                "categories": [
                    {
                        "name": "Thali",
                        "items": [
                            {"name": "Veg Thali", "price": 120, "food_type": "VEG", "is_bestseller": True, "description": "Dal, sabzi, roti (4), rice, raita, papad, sweet"},
                            {"name": "Special Thali", "price": 180, "food_type": "VEG", "description": "Paneer, dal makhani, 2 sabzi, roti (5), rice, raita, gulab jamun"},
                        ],
                    },
                    {
                        "name": "Main Course",
                        "items": [
                            {"name": "Paneer Butter Masala", "price": 160, "food_type": "VEG", "is_bestseller": True, "description": "Rich creamy paneer in tomato gravy"},
                            {"name": "Dal Makhani", "price": 130, "food_type": "VEG", "description": "Slow cooked black lentils with butter and cream"},
                            {"name": "Aloo Gobi", "price": 100, "food_type": "VEG", "description": "Potato and cauliflower dry curry"},
                            {"name": "Chole Bhature", "price": 90, "food_type": "VEG", "is_bestseller": True, "description": "Spicy chickpea curry with fried bread"},
                        ],
                    },
                    {
                        "name": "Breads",
                        "items": [
                            {"name": "Butter Naan", "price": 30, "food_type": "VEG"},
                            {"name": "Garlic Naan", "price": 40, "food_type": "VEG"},
                            {"name": "Tandoori Roti", "price": 15, "food_type": "VEG"},
                            {"name": "Lachha Paratha", "price": 35, "food_type": "VEG"},
                        ],
                    },
                ],
            },
            {
                "name": "Sharma Biryani House",
                "description": "Famous Lucknowi style biryani and kebabs",
                "cuisine_type": "BIRYANI",
                "phone": "+911234500002",
                "address": "Station Road, Near Railway Station, Harraiya",
                "city": "Harraiya",
                "state": "Uttar Pradesh",
                "pincode": "272155",
                "latitude": 26.7695,
                "longitude": 82.5410,
                "rating": 4.5,
                "total_ratings": 520,
                "avg_delivery_time": 35,
                "delivery_fee": 20,
                "min_order_amount": 149,
                "is_veg_only": False,
                "categories": [
                    {
                        "name": "Biryani",
                        "items": [
                            {"name": "Chicken Biryani", "price": 180, "food_type": "NON_VEG", "is_bestseller": True, "description": "Lucknowi dum biryani with tender chicken"},
                            {"name": "Mutton Biryani", "price": 250, "food_type": "NON_VEG", "description": "Slow cooked mutton biryani with aromatic spices"},
                            {"name": "Veg Biryani", "price": 130, "food_type": "VEG", "description": "Fragrant basmati rice with mixed vegetables"},
                            {"name": "Egg Biryani", "price": 140, "food_type": "EGG", "description": "Biryani with boiled eggs and spices"},
                        ],
                    },
                    {
                        "name": "Kebabs",
                        "items": [
                            {"name": "Chicken Seekh Kebab (4pc)", "price": 160, "food_type": "NON_VEG", "is_bestseller": True, "description": "Minced chicken kebabs grilled on skewers"},
                            {"name": "Mutton Galouti Kebab", "price": 220, "food_type": "NON_VEG", "description": "Melt-in-mouth Lucknowi mutton kebabs"},
                            {"name": "Paneer Tikka (6pc)", "price": 140, "food_type": "VEG", "description": "Marinated paneer grilled in tandoor"},
                        ],
                    },
                    {
                        "name": "Drinks",
                        "items": [
                            {"name": "Masala Chaas", "price": 30, "food_type": "VEG"},
                            {"name": "Mango Lassi", "price": 50, "food_type": "VEG", "is_bestseller": True},
                            {"name": "Cold Drink (500ml)", "price": 40, "food_type": "VEG"},
                        ],
                    },
                ],
            },
            {
                "name": "Ganga Fast Food",
                "description": "Quick bites - burgers, momos, chowmein and more",
                "cuisine_type": "FAST_FOOD",
                "phone": "+911234500003",
                "address": "Bus Stand Road, Harraiya",
                "city": "Harraiya",
                "state": "Uttar Pradesh",
                "pincode": "272155",
                "latitude": 26.7670,
                "longitude": 82.5350,
                "rating": 4.0,
                "total_ratings": 180,
                "avg_delivery_time": 20,
                "delivery_fee": 10,
                "min_order_amount": 79,
                "is_veg_only": False,
                "categories": [
                    {
                        "name": "Momos",
                        "items": [
                            {"name": "Veg Steam Momos (8pc)", "price": 60, "food_type": "VEG", "is_bestseller": True, "description": "Steamed dumplings with spicy chutney"},
                            {"name": "Chicken Momos (8pc)", "price": 80, "food_type": "NON_VEG", "is_bestseller": True, "description": "Juicy chicken filled steamed momos"},
                            {"name": "Veg Fried Momos (8pc)", "price": 70, "food_type": "VEG", "description": "Crispy fried vegetable momos"},
                            {"name": "Tandoori Momos (8pc)", "price": 90, "food_type": "VEG", "description": "Grilled momos with tandoori masala"},
                        ],
                    },
                    {
                        "name": "Chinese",
                        "items": [
                            {"name": "Veg Chowmein", "price": 60, "food_type": "VEG", "description": "Stir fried noodles with vegetables"},
                            {"name": "Chicken Chowmein", "price": 80, "food_type": "NON_VEG"},
                            {"name": "Veg Fried Rice", "price": 70, "food_type": "VEG"},
                            {"name": "Manchurian (Dry)", "price": 90, "food_type": "VEG"},
                        ],
                    },
                    {
                        "name": "Burgers & Rolls",
                        "items": [
                            {"name": "Aloo Tikki Burger", "price": 50, "food_type": "VEG", "is_bestseller": True},
                            {"name": "Chicken Burger", "price": 80, "food_type": "NON_VEG"},
                            {"name": "Paneer Roll", "price": 70, "food_type": "VEG"},
                            {"name": "Egg Roll", "price": 60, "food_type": "EGG"},
                        ],
                    },
                ],
            },
            {
                "name": "Hari Om Sweets & Restaurant",
                "description": "Pure vegetarian sweets, snacks and South Indian",
                "cuisine_type": "SOUTH_INDIAN",
                "phone": "+911234500004",
                "address": "Kachahri Road, Harraiya",
                "city": "Harraiya",
                "state": "Uttar Pradesh",
                "pincode": "272155",
                "latitude": 26.7700,
                "longitude": 82.5395,
                "rating": 4.2,
                "total_ratings": 310,
                "avg_delivery_time": 30,
                "delivery_fee": 0,
                "min_order_amount": 99,
                "is_veg_only": True,
                "categories": [
                    {
                        "name": "South Indian",
                        "items": [
                            {"name": "Masala Dosa", "price": 70, "food_type": "VEG", "is_bestseller": True, "description": "Crispy dosa with potato filling, sambar & chutney"},
                            {"name": "Idli Sambar (3pc)", "price": 50, "food_type": "VEG", "description": "Soft steamed rice cakes with sambar"},
                            {"name": "Uttapam", "price": 60, "food_type": "VEG", "description": "Thick pancake with vegetables"},
                            {"name": "Medu Vada (2pc)", "price": 40, "food_type": "VEG"},
                        ],
                    },
                    {
                        "name": "Sweets",
                        "items": [
                            {"name": "Gulab Jamun (2pc)", "price": 40, "food_type": "VEG", "is_bestseller": True},
                            {"name": "Rasgulla (2pc)", "price": 40, "food_type": "VEG"},
                            {"name": "Jalebi (250gm)", "price": 60, "food_type": "VEG"},
                            {"name": "Rabri", "price": 50, "food_type": "VEG"},
                        ],
                    },
                    {
                        "name": "Snacks",
                        "items": [
                            {"name": "Samosa (2pc)", "price": 20, "food_type": "VEG", "is_bestseller": True},
                            {"name": "Kachori (2pc)", "price": 30, "food_type": "VEG"},
                            {"name": "Pav Bhaji", "price": 70, "food_type": "VEG"},
                            {"name": "Bread Pakora (2pc)", "price": 30, "food_type": "VEG"},
                        ],
                    },
                ],
            },
            {
                "name": "Pizza Point",
                "description": "Wood-fired pizzas and Italian bites with Indian twist",
                "cuisine_type": "PIZZA",
                "phone": "+911234500005",
                "address": "MG Road, Harraiya",
                "city": "Harraiya",
                "state": "Uttar Pradesh",
                "pincode": "272155",
                "latitude": 26.7685,
                "longitude": 82.5365,
                "rating": 3.9,
                "total_ratings": 95,
                "avg_delivery_time": 30,
                "delivery_fee": 25,
                "min_order_amount": 149,
                "is_veg_only": False,
                "categories": [
                    {
                        "name": "Pizzas",
                        "items": [
                            {"name": "Margherita Pizza", "price": 149, "food_type": "VEG", "description": "Classic tomato, mozzarella and basil"},
                            {"name": "Paneer Tikka Pizza", "price": 199, "food_type": "VEG", "is_bestseller": True, "description": "Tandoori paneer with onion and capsicum"},
                            {"name": "Chicken Tikka Pizza", "price": 249, "food_type": "NON_VEG", "is_bestseller": True, "description": "Spicy chicken tikka with jalapeños"},
                            {"name": "Veg Supreme", "price": 219, "food_type": "VEG", "description": "Loaded with mushroom, capsicum, olive, onion, corn"},
                        ],
                    },
                    {
                        "name": "Sides",
                        "items": [
                            {"name": "Garlic Bread (4pc)", "price": 79, "food_type": "VEG"},
                            {"name": "Cheesy Fries", "price": 99, "food_type": "VEG", "is_bestseller": True},
                            {"name": "Chicken Wings (6pc)", "price": 149, "food_type": "NON_VEG"},
                        ],
                    },
                    {
                        "name": "Beverages",
                        "items": [
                            {"name": "Cold Coffee", "price": 59, "food_type": "VEG"},
                            {"name": "Mojito", "price": 69, "food_type": "VEG"},
                            {"name": "Brownie with Ice Cream", "price": 99, "food_type": "VEG", "is_bestseller": True},
                        ],
                    },
                ],
            },
        ]

        for r_data in restaurants_data:
            categories = r_data.pop("categories")
            restaurant = Restaurant.objects.create(owner=owner, **r_data)
            self.stdout.write(f"  Created restaurant: {restaurant.name}")

            total_items = 0
            for sort_order, cat_data in enumerate(categories):
                items = cat_data["items"]
                category = MenuCategory.objects.create(
                    restaurant=restaurant,
                    name=cat_data["name"],
                    sort_order=sort_order,
                )

                for item_data in items:
                    MenuItem.objects.create(
                        category=category,
                        name=item_data["name"],
                        price=item_data["price"],
                        food_type=item_data.get("food_type", "VEG"),
                        is_bestseller=item_data.get("is_bestseller", False),
                        description=item_data.get("description", ""),
                        preparation_time=15,
                    )
                total_items += len(items)

            self.stdout.write(f"    Added {total_items} menu items")

        self.stdout.write(self.style.SUCCESS(f"Seeded {len(restaurants_data)} restaurants successfully!"))
