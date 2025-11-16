package com.firstapp.myapplication.models

data class ProgressResponseDto(
    val id: Int,
    val scheduleId: Int,
    val date: String,
    val logged_time: String?,
    val notes: String?,
    val is_completed: Boolean,
    val created_at: String,
    val updated_at: String
)