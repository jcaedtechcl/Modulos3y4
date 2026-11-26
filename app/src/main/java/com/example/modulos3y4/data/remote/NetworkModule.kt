package com.example.modulos3y4.data.remote

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Proveedor del cliente de Retrofit. Es un `object` con inicializacion perezosa
 * para crear una sola instancia de Retrofit/OkHttp en toda la aplicacion: cada
 * instancia nueva abriria su propio pool de conexiones y de hilos.
 */
object NetworkModule {

    private const val BASE_URL = "http://api.open-notify.org/"

    // ignoreUnknownKeys evita que la app falle si la API agrega campos nuevos.
    private val json = Json { ignoreUnknownKeys = true }

    val issApi: IssApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(IssApi::class.java)
    }
}
