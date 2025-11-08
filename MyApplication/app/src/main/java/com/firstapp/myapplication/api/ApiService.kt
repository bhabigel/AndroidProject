package com.firstapp.myapplication.api

import com.firstapp.myapplication.model.ScheduleResponseDto
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {
    @GET("schedule/day")
    suspend fun getSchedulesByDay(@Query("date") date: String? = null): List<ScheduleResponseDto>

    companion object {
        private const val BASE_URL = "http://10.0.2.2:3000/" // For Android emulator

        fun create(): ApiService {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService::class.java)
        }
    }
}