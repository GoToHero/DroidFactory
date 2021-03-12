package ru.androidacademy.droidfactory.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object DevelopersLifeApi {
    val retrofitService: DevelopersLifeApiService by lazy {
        retrofit.create(DevelopersLifeApiService::class.java)
    }
}

private const val BASE_URL = "https://developerslife.ru/"

private val retrofit = Retrofit.Builder()
    .addConverterFactory(GsonConverterFactory.create())
    .baseUrl(BASE_URL)
    .build()
