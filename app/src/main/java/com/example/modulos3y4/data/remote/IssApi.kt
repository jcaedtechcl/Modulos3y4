package com.example.modulos3y4.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET

/**
 * Respuesta de http://api.open-notify.org/iss-now.json
 *
 * {
 *   "timestamp": 1764100000,
 *   "message": "success",
 *   "iss_position": { "latitude": "-12.3456", "longitude": "123.4567" }
 * }
 *
 * Ojo: la API devuelve la latitud y la longitud como texto, no como numero.
 */
@Serializable
data class IssNowResponse(
    val timestamp: Long,
    val message: String,
    @SerialName("iss_position") val issPosition: IssPositionDto
)

@Serializable
data class IssPositionDto(
    val latitude: String,
    val longitude: String
)

interface IssApi {
    @GET("iss-now.json")
    suspend fun getIssNow(): IssNowResponse
}
