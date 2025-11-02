package com.firstapp.myapplication.network

import com.google.gson.annotations.SerializedName

data class SignInRequest(
    val email: String,
    val password: String
)

data class Tokens(
    @SerializedName("accessToken") val accessToken: String,
    @SerializedName("refreshToken") val refreshToken: String
)

data class AuthResponse(
    val message: String?,
    val user: Any?,
    val tokens: Tokens?
)

data class ScheduleResponseDto(
    val id: Long,
    val start_time: String?,
    val status: String?,
    val notes: String?
)
