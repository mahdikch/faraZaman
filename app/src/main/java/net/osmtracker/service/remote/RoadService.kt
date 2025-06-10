package net.osmtracker.service.remote

import net.osmtracker.data.model.RoadData
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface RoadService {
    @GET("api/GisLayers/DataLayerRoadByPoint")
    suspend fun getRoadData(
        @Query("lat") latitude: Double,
        @Query("lng") longitude: Double,
        @Query("buffer") buffer: Int = 10,
        @Header("Authorization") token: String
    ): List<RoadData>
} 