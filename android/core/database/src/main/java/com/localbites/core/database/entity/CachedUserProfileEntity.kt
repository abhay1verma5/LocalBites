package com.localbites.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_user_profile")
data class CachedUserProfileEntity(
    @PrimaryKey val id: Int,
    val phone: String,
    val firstName: String?,
    val lastName: String?,
    val email: String?,
    val role: String,
    val cachedAt: Long = System.currentTimeMillis(),
)
