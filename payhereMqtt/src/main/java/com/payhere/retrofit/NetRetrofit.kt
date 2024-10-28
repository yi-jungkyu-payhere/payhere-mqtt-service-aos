package com.payhere.retrofit

import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class NetRetrofit {
    var slog: String? = null
    val gson = GsonBuilder().setLenient().create()
    var interceptor =
        HttpLoggingInterceptor(
            object : HttpLoggingInterceptor.Logger {
                override fun log(s: String) {
                }
            },
        ).setLevel(HttpLoggingInterceptor.Level.BODY)
//    }).setLevel(HttpLoggingInterceptor.Level.BASIC)

    fun getNetRetrofit(): RetrofitService = service

    var defaultHttpClient =
        OkHttpClient
            .Builder() // .connectionPool(cPool)
            //            .connectTimeout(1, TimeUnit.SECONDS)
            //            .readTimeout(5, TimeUnit.SECONDS)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
//        .cookieJar(App.cookieJar)
            .addInterceptor(interceptor)
//        .addInterceptor(HeaderInterceptor(App.token))
            .build()

    var retrofit: Retrofit =
        Retrofit
            .Builder()
            .baseUrl("https://device-monitoring.payhere.in")
            .addConverterFactory(GsonConverterFactory.create(gson)) // 파싱등록
            .client(defaultHttpClient)
            .build()
    var service = retrofit.create(RetrofitService::class.java)
}
