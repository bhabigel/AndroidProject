package com.firstapp.myapplication.models

data class ProfileResponseDto(
    val id: Int,
    val email: String,
    val username: String?,
    val description: String?,
    val profileImageUrl: String?,
    val created_at: String,
    val updated_at: String
)