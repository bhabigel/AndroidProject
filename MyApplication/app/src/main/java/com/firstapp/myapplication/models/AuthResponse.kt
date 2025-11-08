package com.firstapp.myapplication.models

data class AuthResponse(
    val tokens: Tokens,
    val user: User
)

data class Tokens(
    val accessToken: String,
    val refreshToken: String
)

data class User(
    val id: Int,
    val username: String,
    val email: String,
    val profileImageUrl: String?
)