package com.pelypets.todolist.data.remote

import com.pelypets.todolist.data.remote.dto.ErrorResponseDto
import com.pelypets.todolist.domain.DomainResult
import com.pelypets.todolist.domain.error.DomainError
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException

internal val DataJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

internal suspend inline fun <T> safeApiCall(crossinline block: suspend () -> T): DomainResult<T> = try {
    DomainResult.Success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: HttpException) {
    DomainResult.Failure(e.toDomainError())
} catch (e: IOException) {
    DomainResult.Failure(DomainError.Network)
} catch (e: Exception) {
    DomainResult.Failure(DomainError.Unknown(e.message))
}

internal fun HttpException.toDomainError(): DomainError {
    val message = runCatching {
        response()?.errorBody()?.string()?.let { DataJson.decodeFromString<ErrorResponseDto>(it).message }
    }.getOrNull()
    return when (code()) {
        400 -> DomainError.Validation(serverMessage = message)
        401 -> DomainError.Unauthorized
        404 -> DomainError.NotFound
        409 -> DomainError.Conflict
        429 -> DomainError.TooManyRequests
        else -> DomainError.Unknown(message ?: "HTTP ${code()}")
    }
}
