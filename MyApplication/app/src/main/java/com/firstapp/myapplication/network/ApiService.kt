package com.firstapp.myapplication.network

import com.firstapp.myapplication.models.HabitResponseDto
import com.firstapp.myapplication.models.ScheduleRequestDto
import com.firstapp.myapplication.models.ScheduleResponseDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @POST("auth/local/signin")
    suspend fun signin(@Body body: SignInRequest): Response<AuthResponse>

    @Multipart
    @POST("auth/local/signup")
    suspend fun signup(
        @Part("username") username: RequestBody,
        @Part("email") email: RequestBody,
        @Part("password") password: RequestBody,
        @Part profileImage: MultipartBody.Part? = null,
    ): Response<AuthResponse>

    @POST("auth/local/logout")
    suspend fun logout(): Response<Void>

    @POST("auth/reset-password-via-email")
    suspend fun resetPasswordViaEmail(@Body body: Map<String, String>): Response<Map<String, String>>

    @GET("profile")
    suspend fun getProfile(): Response<Map<String, Any>>

    @PATCH("profile")
    suspend fun updateProfile(@Body body: Map<String, Any>): Response<Map<String, Any>>

    @GET("habit")
    suspend fun listHabits(): Response<List<HabitResponseDto>>

    @GET("habit/categories")
    suspend fun listHabitCategories(): Response<List<com.firstapp.myapplication.models.HabitCategoryResponseDto>>

    @POST("habit")
    suspend fun createHabit(@Body body: Map<String, @kotlin.jvm.JvmSuppressWildcards Any>): Response<HabitResponseDto>

    @GET("schedule/day")
    suspend fun getSchedulesByDay(@Query("date") date: String? = null): Response<List<ScheduleResponseDto>>

    @GET("schedule/{id}")
    suspend fun getScheduleById(@Path("id") id: Int): Response<ScheduleResponseDto>

    @PATCH("schedule/{id}")
    suspend fun updateSchedule(@Path("id") id: Int, @Body body: Map<String, Any>): Response<ScheduleResponseDto>

    @DELETE("schedule/{id}")
    suspend fun deleteSchedule(@Path("id") id: Int): Response<Void>

    @POST("schedule/custom")
    suspend fun createCustomSchedule(@Body body: ScheduleRequestDto): Response<ScheduleResponseDto>

    @POST("schedule/recurring")
    suspend fun createRecurringSchedule(@Body request: ScheduleRequestDto): Response<List<ScheduleResponseDto>>

    @POST("schedule/recurring/weekdays")
    suspend fun createWeekdayRecurringSchedule(@Body request: ScheduleRequestDto): Response<List<ScheduleResponseDto>>

    @POST("progress")
    suspend fun createProgress(@Body body: Map<String, Any>): Response<Map<String, Any>>

    @POST("auth/local/refresh")
    suspend fun refresh(@Body body: Map<String, String>): Response<AuthResponse>

    @GET("habit/user/{userId}")
    suspend fun getHabitsByUser(@Path("userId") id: Int): Response<List<Map<String, Any>>>

    @Multipart
    @POST("profile/upload-profile-image")
    suspend fun uploadProfileImage(@Part file: MultipartBody.Part): Response<Map<String, Any>>
}
