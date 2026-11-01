package com.pelypets.todolist.di

import com.pelypets.todolist.BuildConfig
import com.pelypets.todolist.data.remote.ApiConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun apiConfig(): ApiConfig = ApiConfig(baseUrl = BuildConfig.API_BASE_URL, enableLogging = BuildConfig.DEBUG)

    /** Not a singleton: the user may change the time zone while the app is running. */
    @Provides
    fun clock(): Clock = Clock.systemDefaultZone()
}
