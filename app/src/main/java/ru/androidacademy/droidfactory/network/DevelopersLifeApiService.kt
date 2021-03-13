package ru.androidacademy.droidfactory.network

import retrofit2.http.GET
import retrofit2.http.Path

interface DevelopersLifeApiService {
    @GET("latest/{page}?json=true")
    suspend fun getProperties(@Path("page") page: Int):
            DevelopersLifePropertyContainer
}
