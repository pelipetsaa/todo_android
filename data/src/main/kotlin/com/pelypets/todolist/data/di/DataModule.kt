package com.pelypets.todolist.data.di

import com.pelypets.todolist.data.local.KeystoreTokenStorage
import com.pelypets.todolist.data.local.TokenStorage
import com.pelypets.todolist.data.remote.ApiConfig
import com.pelypets.todolist.data.remote.DataJson
import com.pelypets.todolist.data.remote.api.AuthApi
import com.pelypets.todolist.data.remote.api.TodoApi
import com.pelypets.todolist.data.remote.interceptor.AuthInterceptor
import com.pelypets.todolist.data.remote.interceptor.RetryInterceptor
import com.pelypets.todolist.data.remote.interceptor.TokenAuthenticator
import com.pelypets.todolist.data.repository.AuthRepositoryImpl
import com.pelypets.todolist.data.repository.TodoRepositoryImpl
import com.pelypets.todolist.data.session.SessionManager
import com.pelypets.todolist.domain.repository.AuthRepository
import com.pelypets.todolist.domain.repository.SessionRepository
import com.pelypets.todolist.domain.repository.TodoRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataBindingsModule {

    @Binds
    abstract fun authRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    abstract fun todoRepository(impl: TodoRepositoryImpl): TodoRepository

    @Binds
    abstract fun sessionRepository(impl: SessionManager): SessionRepository

    @Binds
    abstract fun tokenStorage(impl: KeystoreTokenStorage): TokenStorage
}

@Module
@InstallIn(SingletonComponent::class)
internal object CoroutinesModule {

    @Provides
    @IoDispatcher
    fun ioDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Singleton
    @ApplicationScope
    fun applicationScope(@IoDispatcher dispatcher: CoroutineDispatcher): CoroutineScope = CoroutineScope(SupervisorJob() + dispatcher)
}

@Module
@InstallIn(SingletonComponent::class)
internal object NetworkModule {

    /**
     * The free Render instance sleeps after 15 minutes without traffic and holds the first request while it starts,
     * which can take close to a minute; a shorter timeout would turn every first sign-in into a network error.
     */
    private const val CALL_TIMEOUT_SECONDS = 75L
    private const val CONNECT_TIMEOUT_SECONDS = 10L

    @Provides
    @Singleton
    fun okHttpClient(
        config: ApiConfig,
        authInterceptor: AuthInterceptor,
        retryInterceptor: RetryInterceptor,
        authenticator: TokenAuthenticator,
    ): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .addInterceptor(authInterceptor)
        .addInterceptor(retryInterceptor)
        .authenticator(authenticator)
        .apply {
            if (config.enableLogging) {
                addInterceptor(
                    HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BASIC
                        redactHeader("Authorization")
                    },
                )
            }
        }
        .build()

    @Provides
    @Singleton
    fun retrofit(config: ApiConfig, client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(config.baseUrl.trimEnd('/') + "/")
        .client(client)
        .addConverterFactory(DataJson.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun authApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun todoApi(retrofit: Retrofit): TodoApi = retrofit.create(TodoApi::class.java)
}
