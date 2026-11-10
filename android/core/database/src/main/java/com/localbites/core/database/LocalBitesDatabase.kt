package com.localbites.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.localbites.core.database.dao.*
import com.localbites.core.database.entity.*

@Database(
    entities = [
        CartEntity::class,
        CachedRestaurantEntity::class,
        CachedMenuCategoryEntity::class,
        CachedMenuItemEntity::class,
        CachedOrderEntity::class,
        CachedUserProfileEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class LocalBitesDatabase : RoomDatabase() {
    abstract fun cartDao(): CartDao
    abstract fun cachedRestaurantDao(): CachedRestaurantDao
    abstract fun cachedMenuDao(): CachedMenuDao
    abstract fun cachedOrderDao(): CachedOrderDao
    abstract fun cachedUserProfileDao(): CachedUserProfileDao

    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `cached_restaurants` ADD COLUMN `imageUrl` TEXT")
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `cached_restaurants` (
                        `id` INTEGER NOT NULL PRIMARY KEY,
                        `name` TEXT NOT NULL,
                        `cuisineType` TEXT NOT NULL,
                        `image` TEXT,
                        `rating` REAL NOT NULL,
                        `totalRatings` INTEGER NOT NULL,
                        `avgDeliveryTime` INTEGER NOT NULL,
                        `deliveryFee` REAL NOT NULL,
                        `minOrderAmount` REAL NOT NULL,
                        `isVegOnly` INTEGER NOT NULL,
                        `isAcceptingOrders` INTEGER NOT NULL,
                        `city` TEXT,
                        `latitude` REAL,
                        `longitude` REAL,
                        `cachedAt` INTEGER NOT NULL
                    )"""
                )
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `cached_menu_categories` (
                        `id` INTEGER NOT NULL PRIMARY KEY,
                        `restaurantId` INTEGER NOT NULL,
                        `name` TEXT NOT NULL,
                        `sortOrder` INTEGER NOT NULL,
                        `cachedAt` INTEGER NOT NULL
                    )"""
                )
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `cached_menu_items` (
                        `id` INTEGER NOT NULL PRIMARY KEY,
                        `categoryId` INTEGER NOT NULL,
                        `restaurantId` INTEGER NOT NULL,
                        `name` TEXT NOT NULL,
                        `description` TEXT,
                        `price` REAL NOT NULL,
                        `discountedPrice` REAL,
                        `effectivePrice` REAL NOT NULL,
                        `image` TEXT,
                        `foodType` TEXT NOT NULL,
                        `isAvailable` INTEGER NOT NULL,
                        `isBestseller` INTEGER NOT NULL,
                        `preparationTime` INTEGER NOT NULL,
                        `cachedAt` INTEGER NOT NULL
                    )"""
                )
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `cached_orders` (
                        `id` INTEGER NOT NULL PRIMARY KEY,
                        `restaurantName` TEXT,
                        `restaurantImage` TEXT,
                        `status` TEXT NOT NULL,
                        `total` REAL NOT NULL,
                        `placedAt` TEXT,
                        `itemsSummaryJson` TEXT NOT NULL,
                        `cachedAt` INTEGER NOT NULL
                    )"""
                )
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `cached_user_profile` (
                        `id` INTEGER NOT NULL PRIMARY KEY,
                        `phone` TEXT NOT NULL,
                        `firstName` TEXT,
                        `lastName` TEXT,
                        `email` TEXT,
                        `role` TEXT NOT NULL,
                        `cachedAt` INTEGER NOT NULL
                    )"""
                )
            }
        }
    }
}
