package com.firstapp.myapplication.models

data class UpdateScheduleDto(
    val start_time: String? = null,
    val end_time: String? = null,
    val duration_minutes: Int? = null,
    val status: String? = null, // "Planned", "Completed", "Skipped"
    val date: String? = null,
    val is_custom: Boolean? = null,
    val participantIds: List<Int>? = null,
    val notes: String? = null
)