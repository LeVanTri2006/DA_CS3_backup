package com.example.da_cuoiky.network

import com.google.gson.GsonBuilder
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object RetrofitClient {
    // 10.0.2.2 trỏ về localhost của máy tính khi dùng máy ảo Android
    //private const val BASE_URL = "http://10.0.2.2/WEB_ADMIN-main/"
//    private const val BASE_URL = "http://192.168.2.147/WEB_ADMIN-main/"
    //private const val BASE_URL = "http://nguyenpkm2403.id.vn/"
    private const val BASE_URL = "http://enrich-tile-shale.ngrok-free.dev/WEB_ADMIN-main/"

    private val gson = GsonBuilder()
        .setLenient()
        .create()

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val instance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ApiService::class.java)
    }
}
