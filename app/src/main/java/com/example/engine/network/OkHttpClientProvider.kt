package com.example.engine.network

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object OkHttpClientProvider {
  val client: OkHttpClient by lazy {
    OkHttpClient.Builder()
      .connectTimeout(10, TimeUnit.SECONDS)
      .readTimeout(15, TimeUnit.SECONDS)
      .writeTimeout(10, TimeUnit.SECONDS)
      .callTimeout(20, TimeUnit.SECONDS)
      .retryOnConnectionFailure(true)
      .followRedirects(false)
      .build()
  }
}
