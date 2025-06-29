package net.osmtracker.data.api

import net.osmtracker.data.model.FormDataApiRequest
import net.osmtracker.data.model.FormDataResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface FormDataApiService {
    @POST("api/BillOriginViEvent/FormData")
    suspend fun getFormData(
        @Header("Content-Type") contentType: String = "application/json",
        @Header("Authorization") authorization: String,
        @Body request: FormDataApiRequest
    ): Response<FormDataResponse>
} 