// در فایل ViolationApiService.kt

package net.osmtracker.service.remote

import net.osmtracker.data.model.SubmitViolationResponse
import net.osmtracker.data.model.ViolationSuccessResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ViolationApiService {

    @Multipart
    @POST("api/BillOriginViEvent/Create")
    suspend fun createViolation(
        @Header("Authorization") token: String,
        @Part model: MultipartBody.Part // ما درخواست را به صورت multipart ارسال می‌کنیم
    ): Response<ViolationSuccessResponse>
}