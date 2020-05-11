package com.andruid.magic.game.server

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "http://192.168.43.200:5000/"

    private var INSTANCE = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    fun getRetrofitInstance(): Retrofit = INSTANCE
}