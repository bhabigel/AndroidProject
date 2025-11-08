package com.firstapp.myapplication.model

data class ScheduleResponseDto(
    val id: String,
    val habitId: String,
    val startTime: String,  // ISO time format
    val status: String,     // "Planned", "Completed", or "Skipped"
    val habit: HabitDto
)

data class HabitDto(
    val id: String,
    val name: String,
    val type: String,
    val description: String?
)