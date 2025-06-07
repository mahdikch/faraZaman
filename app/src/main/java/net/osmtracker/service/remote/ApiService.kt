package net.osmtracker.service.remote

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface ApiService {
    @GET("api/GisLayers/DataLayerRoadByPoint")
    suspend fun getRoadByPoint(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("buffer") buffer: Int,
        @Header("Authorization") auth: String
    ): ResponseBody
}