package com.pelypets.todolist.data.remote.api

import com.pelypets.todolist.data.remote.dto.EmailCredentialsDto
import com.pelypets.todolist.data.remote.dto.GoogleLoginDto
import com.pelypets.todolist.data.remote.dto.RefreshTokenDto
import com.pelypets.todolist.data.remote.dto.TodoDto
import com.pelypets.todolist.data.remote.dto.TodoInputDto
import com.pelypets.todolist.data.remote.dto.TokenPairDto
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

object ApiRoutes {
    const val BASE = "api/v1"
    const val AUTH_PREFIX = "/$BASE/auth"
}

interface AuthApi {
    @POST("${ApiRoutes.BASE}/auth/login")
    suspend fun login(@Body body: EmailCredentialsDto): TokenPairDto

    @POST("${ApiRoutes.BASE}/auth/register")
    suspend fun register(@Body body: EmailCredentialsDto): TokenPairDto

    @POST("${ApiRoutes.BASE}/auth/google")
    suspend fun loginWithGoogle(@Body body: GoogleLoginDto): TokenPairDto

    @POST("${ApiRoutes.BASE}/auth/logout")
    suspend fun logout(@Body body: RefreshTokenDto)

    /** Blocking on purpose: it is called from OkHttp's authenticator thread. */
    @POST("${ApiRoutes.BASE}/auth/refresh")
    fun refresh(@Body body: RefreshTokenDto): Call<TokenPairDto>
}

interface TodoApi {
    @GET("${ApiRoutes.BASE}/todos")
    suspend fun getTodos(@Query("from") from: String, @Query("to") to: String): List<TodoDto>

    @GET("${ApiRoutes.BASE}/todos/{id}")
    suspend fun getTodo(@Path("id") id: String): TodoDto

    @POST("${ApiRoutes.BASE}/todos")
    suspend fun create(@Body body: TodoInputDto): TodoDto

    @PUT("${ApiRoutes.BASE}/todos/{id}")
    suspend fun update(@Path("id") id: String, @Body body: TodoInputDto): TodoDto

    @DELETE("${ApiRoutes.BASE}/todos/{id}")
    suspend fun delete(@Path("id") id: String)
}
