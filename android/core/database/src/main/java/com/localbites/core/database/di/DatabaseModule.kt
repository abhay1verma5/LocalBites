package com.localbites.core.database.di

import android.content.Context
import androidx.room.Room
import com.localbites.core.database.LocalBitesDatabase
import com.localbites.core.database.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LocalBitesDatabase =
        Room.databaseBuilder(
            context,
            LocalBitesDatabase::class.java,
            "localbites_db"
        )
            .addMigrations(LocalBitesDatabase.MIGRATION_1_2)
            .build()

    @Provides
    fun provideCartDao(db: LocalBitesDatabase): CartDao = db.cartDao()

    @Provides
    fun provideCachedRestaurantDao(db: LocalBitesDatabase): CachedRestaurantDao =
        db.cachedRestaurantDao()

    @Provides
    fun provideCachedMenuDao(db: LocalBitesDatabase): CachedMenuDao = db.cachedMenuDao()

    @Provides
    fun provideCachedOrderDao(db: LocalBitesDatabase): CachedOrderDao = db.cachedOrderDao()

    @Provides
    fun provideCachedUserProfileDao(db: LocalBitesDatabase): CachedUserProfileDao =
        db.cachedUserProfileDao()
}
