package com.firstapp.myapplication.models

data class ScheduleRequestDto(
    val habitId: Int,
    val date: String? = null,
    val start_time: String,
    val end_time: String? = null,
    val duration_minutes: Int? = null,
    val is_custom: Boolean = true,
    val repeatPattern: String? = null,
    val repeatDays: Int? = null,
    val participantIds: List<Int>? = null,
    val notes: String? = null,
    val daysOfWeek: List<Int>? = null,
    val numberOfWeeks: Int? = null
)