package com.firstapp.myapplication.models

data class HabitCategoryResponseDto(
    val id: Int,
    val name: String,
    val iconUrl: String? = null
)
