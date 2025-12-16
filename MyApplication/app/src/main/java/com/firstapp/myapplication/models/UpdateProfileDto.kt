package com.firstapp.myapplication.models

data class UpdateProfileDto(
    val username: String? = null,
    val description: String? = null,
    val city: String? = null
)