package com.firstapp.myapplication.models

data class HabitResponseDto(
    val id: Int,
    val name: String,
    val description: String? = null,
    val created_at: String,
    val updated_at: String
)