package com.localbites.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.localbites.core.database.entity.CachedUserProfileEntity

@Dao
interface CachedUserProfileDao {

    @Query("SELECT * FROM cached_user_profile LIMIT 1")
    suspend fun getProfile(): CachedUserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: CachedUserProfileEntity)

    @Query("DELETE FROM cached_user_profile")
    suspend fun clearProfile()
}
