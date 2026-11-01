package com.pelypets.todolist.data.remote.dto

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.Instant

/** Mirrors the backend contract (`core` module of the ToDoList project): ISO-8601 instants in UTC. */
object InstantIsoSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("java.time.Instant", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: Instant) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder): Instant = Instant.parse(decoder.decodeString())
}

@Serializable
data class EmailCredentialsDto(val email: String, val password: String)

@Serializable
data class GoogleLoginDto(val idToken: String)

@Serializable
data class RefreshTokenDto(val refreshToken: String)

@Serializable
data class TokenPairDto(val accessToken: String, val refreshToken: String, val expiresInSeconds: Long)

@Serializable
enum class TodoStatusDto { TODO, IN_PROGRESS, DONE }

@Serializable
data class TodoInputDto(
    val title: String,
    val description: String = "",
    @Serializable(with = InstantIsoSerializer::class) val dueAt: Instant,
    val status: TodoStatusDto = TodoStatusDto.TODO,
)

@Serializable
data class TodoDto(
    val id: String,
    val title: String,
    val description: String,
    @Serializable(with = InstantIsoSerializer::class) val dueAt: Instant,
    val status: TodoStatusDto,
    @Serializable(with = InstantIsoSerializer::class) val createdAt: Instant,
    @Serializable(with = InstantIsoSerializer::class) val updatedAt: Instant,
)

@Serializable
data class ErrorResponseDto(val code: String, val message: String)
