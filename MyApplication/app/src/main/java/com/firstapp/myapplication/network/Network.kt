package com.firstapp.myapplication.network

import com.firstapp.myapplication.auth.AuthManager
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object Network {
    // Default base URL points to host machine when running in emulator.
    // Backend README shows default backend port 8080; use 10.0.2.2 to reach host from emulator.
    private const val BASE_URL = "http://10.0.2.2:8080/"
    // expose refresh URL for other components
    val REFRESH_URL = BASE_URL + "auth/local/refresh"

    private val authInterceptor = Interceptor { chain ->
        val reqBuilder = chain.request().newBuilder()
        val token = AuthManager.accessToken
        if (!token.isNullOrEmpty()) {
            reqBuilder.addHeader("Authorization", "Bearer $token")
        }
        chain.proceed(reqBuilder.build())
    }

    // Authenticator that attempts to refresh access token using the refresh token when a 401 occurs.
    private val tokenAuthenticator = Authenticator { route: Route?, response: Response ->
        // Prevent infinite loops
        if (response.request.header("Authorization") != null && responseCount(response) >= 2) {
            return@Authenticator null
        }

        val refresh = AuthManager.refreshToken
        if (refresh.isNullOrEmpty()) return@Authenticator null

        try {
            // Make a synchronous call to refresh tokens
            val refreshReq = Request.Builder()
                .url(REFRESH_URL)
                .post(okhttp3.RequestBody.create(null, ""))
                .addHeader("Authorization", "Bearer $refresh")
                .build()

            val tmpClient = OkHttpClient.Builder().addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }).build()
            val resp = tmpClient.newCall(refreshReq).execute()
            if (!resp.isSuccessful) {
                return@Authenticator null
            }
            val body = resp.body?.string() ?: return@Authenticator null
            val json = JSONObject(body)
            val tokens = json.optJSONObject("tokens")
            val newAt = tokens?.optString("accessToken")
            val newRt = tokens?.optString("refreshToken")
            if (!newAt.isNullOrEmpty()) {
                AuthManager.save(newAt, newRt)
                // Retry original request with new access token
                return@Authenticator response.request.newBuilder()
                    .header("Authorization", "Bearer $newAt")
                    .build()
            }
        } catch (e: Exception) {
            // ignore
        }
        null
    }

    /**
     * Attempt to refresh tokens using the given refresh token.
     * Returns Pair(accessToken, refreshToken) on success or null on failure.
     * This performs a synchronous network call and should be run off the UI thread.
     */
    fun refreshWithRefreshTokenBlocking(refresh: String): Pair<String, String>? {
        try {
            val refreshReq = Request.Builder()
                .url(REFRESH_URL)
                .post(okhttp3.RequestBody.create(null, ""))
                .addHeader("Authorization", "Bearer $refresh")
                .build()

            val tmpClient = OkHttpClient.Builder().addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }).build()
            val resp = tmpClient.newCall(refreshReq).execute()
            if (!resp.isSuccessful) return null
            val body = resp.body?.string() ?: return null
            val json = JSONObject(body)
            val tokens = json.optJSONObject("tokens")
            val newAt = tokens?.optString("accessToken")
            val newRt = tokens?.optString("refreshToken")
            if (!newAt.isNullOrEmpty() && !newRt.isNullOrEmpty()) {
                return Pair(newAt, newRt)
            }
        } catch (e: Exception) {
            // ignore and return null
        }
        return null
    }

    private fun responseCount(response: Response): Int {
        var res = response
        var count = 1
        while (res.priorResponse != null) {
            count++
            res = res.priorResponse!!
        }
        return count
    }

    private val client by lazy {
        val logger = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .authenticator(tokenAuthenticator)
            .addInterceptor(logger)
            .build()
    }

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
