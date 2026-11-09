package com.localbites.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.localbites.core.database.dao.CartDao
import com.localbites.core.database.entity.CartEntity

@Database(
    entities = [CartEntity::class],
    version = 1,
    exportSchema = false
)
abstract class LocalBitesDatabase : RoomDatabase() {
    abstract fun cartDao(): CartDao
}
