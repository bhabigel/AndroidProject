package com.firstapp.myapplication.models

data class ScheduleResponseDto(
    val id: Int,
    val habitId: Int,
    val date: String,
    val start_time: String,
    val end_time: String?,
    val duration_minutes: Int?,
    val is_custom: Boolean,
    val status: String?,
    val participantIds: List<Int>?,
    val notes: String?,
    val created_at: String,
    val updated_at: String,
    // New fields for detailed schedule response
    val habit: HabitResponseDto? = null,
    val progress: List<ProgressResponseDto>? = null
)