package net.osmtracker.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import net.osmtracker.data.api.FormDataApiService
import net.osmtracker.data.model.FormDataApiRequest
import net.osmtracker.data.model.FormDataRequest
import net.osmtracker.data.model.FormDataResponse
import net.osmtracker.data.model.RequestData
import retrofit2.Response
import javax.inject.Inject

class FormDataRepository @Inject constructor(
    private val apiService: FormDataApiService,
    private val context: Context,
    private val sharedPreferences: SharedPreferences
) {
    
    companion object {
        private const val BASE_URL = "https://app.tfs.co.ir/"
        private const val ACCESS_TOKEN_KEY = "ACCESS_TOKEN"
    }
    
    private fun getAccessToken(): String? {
        return sharedPreferences.getString(ACCESS_TOKEN_KEY, null)
    }
    
    suspend fun getFormData(formDataRequest: FormDataRequest? = null): Response<FormDataResponse> {
        val accessToken = getAccessToken()
        if (accessToken.isNullOrEmpty()) {
            throw IllegalStateException("Access token not found. Please login first.")
        }
        
        val request = if (formDataRequest != null) {
            FormDataApiRequest(request = formDataRequest)
        } else {
            // Initial request with just tenantId
            val initialRequest = FormDataRequest(
                contractIds = emptyList(),
                organIds = emptyList(),
                billCleaningViolationGroupIds = emptyList(),
                billCleaningViolationIds = emptyList(),
                billCleaningItemGroupIds = emptyList(),
                billCleaningItemIds = emptyList(),
                visitDate = "",
                maxVisitDate = "",
                minVisitDate = "",
                isDeleted = false,
                tenantId = 1,
                billOriginCleaningItemIds = emptyList(),
                billCleaningViolationId = 0,
                contractId = 0,
                organId = 0
            )
            FormDataApiRequest(request = initialRequest)
        }
        
        return apiService.getFormData(
            authorization = "Bearer $accessToken",
            request = request
        )
    }
} 