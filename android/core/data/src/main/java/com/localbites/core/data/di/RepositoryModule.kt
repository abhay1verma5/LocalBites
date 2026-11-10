package com.localbites.core.data.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    // All repositories use @Inject constructor with @Singleton,
    // so Hilt can provide them automatically without explicit @Provides methods.
}
