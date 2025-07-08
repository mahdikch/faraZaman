package net.osmtracker.presentation.viewmodel

import android.app.Application
import android.content.ContentValues
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.osmtracker.data.db.TrackContentProvider
import net.osmtracker.data.model.DropdownItem
import net.osmtracker.data.model.FormDataRequest
import net.osmtracker.data.model.FormDataResponse
import net.osmtracker.data.model.SubmitViolationRequest
import net.osmtracker.data.model.ViolationData
import net.osmtracker.data.model.ViolationErrorResponse
import net.osmtracker.data.repository.FormDataRepository
import net.osmtracker.service.remote.ViolationApiService
import okhttp3.MultipartBody
import retrofit2.Response
import javax.inject.Inject

@HiltViewModel
class FormDataViewModel @Inject constructor(
    private val application: Application,
    private val repository: FormDataRepository,
    private val violationApiService: ViolationApiService,
    private val prefs: SharedPreferences
) : ViewModel() {
    // متغیرهایی برای نگهداری ID های انتخاب شده
    private var selectedOrganId: Int? = null
    private var selectedContractId: Int? = null
    private var selectedViolationGroupId: Int? = null
    private var selectedViolationId: Int? = null
    private var selectedItemGroupId: Int? = null
    private var selectedOriginItemId: Int? = null

    private val _submissionState = MutableLiveData<SubmissionState>()
    val submissionState: LiveData<SubmissionState> = _submissionState

    private val _formData = MutableLiveData<FormDataResponse>()
    val formData: LiveData<FormDataResponse> = _formData
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error
    
    // Current form data request state
    private var currentFormDataRequest = FormDataRequest(
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

    fun saveViolationAsWaypoint(
        trackId: Long,
        latitude: Double,
        longitude: Double,
        violationIdFromServer: String
    ) {
        // اجرای عملیات دیتابیس در یک ترد پس‌زمینه مخصوص I/O
        viewModelScope.launch(Dispatchers.IO) {
            Log.d("ViewModelLog", "Attempting to save waypoint in background for track: $trackId")

            // مقادیری که باید در دیتابیس ذخیره شوند
            val values = ContentValues().apply {
                put(TrackContentProvider.Schema.COL_TRACK_ID, trackId)
                put(TrackContentProvider.Schema.COL_LATITUDE, latitude)
                put(TrackContentProvider.Schema.COL_LONGITUDE, longitude)
                put(TrackContentProvider.Schema.COL_NAME, "تخلف ثبت شده")
                put(TrackContentProvider.Schema.COL_LINK, violationIdFromServer) // ذخیره ID سرور در ستون لینک
                put(TrackContentProvider.Schema.COL_TIMESTAMP, System.currentTimeMillis())
                put(TrackContentProvider.Schema.COL_NBSATELLITES, 0) // Default value to satisfy NOT NULL constraint

            }

            try {
                // استفاده از application context برای دسترسی به ContentResolver
                val correctUri = TrackContentProvider.waypointsUri(trackId)
                val waypointUri = application.contentResolver.insert(correctUri, values)

                if (waypointUri != null) {
                    Log.i("ViewModelLog", "Waypoint saved successfully for violation ID: $violationIdFromServer")
                } else {
                    Log.e("ViewModelLog", "Failed to save waypoint, content resolver returned null URI.")
                }
            } catch (e: Exception) {
                // مدیریت خطاهای احتمالی در زمان ذخیره در دیتابیس
                Log.e("ViewModelLog", "Error saving waypoint to database.", e)
            }
        }
    }
    fun fetchFormData() {
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val response = repository.getFormData()
                if (response.isSuccessful) {
                    response.body()?.let { formData ->
                        _formData.value = formData
                    } ?: run {
                        _error.value = "Empty response from server"
                    }
                } else {
                    _error.value = "Error: ${response.code()} - ${response.message()}"
                }
            } catch (e: IllegalStateException) {
                _error.value = e.message ?: "Access token not available"
            } catch (e: Exception) {
                _error.value = "Network error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchFormDataWithRequest(updatedRequest: FormDataRequest) {
        _isLoading.value = true
        _error.value = null
        
        viewModelScope.launch {
            try {
                val response = repository.getFormData(updatedRequest)
                if (response.isSuccessful) {
                    response.body()?.let { formData ->
                        _formData.value = formData
                        currentFormDataRequest = updatedRequest
                    } ?: run {
                        _error.value = "Empty response from server"
                    }
                } else {
                    _error.value = "Error: ${response.code()} - ${response.message()}"
                }
            } catch (e: IllegalStateException) {
                _error.value = e.message ?: "Access token not available"
            } catch (e: Exception) {
                _error.value = "Network error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun updateOrganSelection(organId: Int) {
        val updatedRequest = currentFormDataRequest.copy(
            organId = organId,
            organIds = listOf(organId)
        )
        fetchFormDataWithRequest(updatedRequest)
    }
    
    fun updateContractSelection(contractId: Int) {
        val updatedRequest = currentFormDataRequest.copy(
            contractId = contractId,
            contractIds = listOf(contractId)
        )
        fetchFormDataWithRequest(updatedRequest)
    }
    
    fun updateViolationGroupSelection(violationGroupId: Int) {
        val updatedRequest = currentFormDataRequest.copy(
            billCleaningViolationGroupIds = listOf(violationGroupId)
        )
        fetchFormDataWithRequest(updatedRequest)
    }
    
    fun updateViolationSelection(violationId: Int) {
        val updatedRequest = currentFormDataRequest.copy(
            billCleaningViolationId = violationId,
            billCleaningViolationIds = listOf(violationId)
        )
        fetchFormDataWithRequest(updatedRequest)
    }
    
    fun updateItemGroupSelection(itemGroupId: Int) {
        val updatedRequest = currentFormDataRequest.copy(
            billCleaningItemGroupIds = listOf(itemGroupId)
        )
        fetchFormDataWithRequest(updatedRequest)
    }
    
    fun getOrgans(): List<DropdownItem> {
        return _formData.value?.response?.organs ?: emptyList()
    }
    
    fun getContracts(): List<DropdownItem> {
        return _formData.value?.response?.contracts ?: emptyList()
    }
    
    fun getViolationGroups(): List<DropdownItem> {
        return _formData.value?.response?.billCleaningViolationGroups ?: emptyList()
    }
    
    fun getViolations(): List<DropdownItem> {
        return _formData.value?.response?.billCleaningViolations ?: emptyList()
    }
    
    fun getItemGroups(): List<DropdownItem> {
        return _formData.value?.response?.billCleaningItemGroups ?: emptyList()
    }
    
    fun getOriginItems(): List<DropdownItem> {
        return _formData.value?.response?.billOriginCleaningItems ?: emptyList()
    }

    fun onOrganSelected(organId: Int) {
        selectedOrganId = organId
        // منطق فیلتر کردن قراردادها...
    }

    fun onContractSelected(contractId: Int) {
        selectedContractId = contractId
        // ...
    }
    fun onViolationGroupIdSelected(organId: Int) {
        selectedViolationGroupId = organId
        // منطق فیلتر کردن قراردادها...
    }

    fun onViolationIdSelected(contractId: Int) {
        selectedViolationId = contractId
        // ...
    }
    fun onItemGroupIdSelected(organId: Int) {
        selectedItemGroupId = organId
        // منطق فیلتر کردن قراردادها...
    }

    fun onOriginItemIdSelected(contractId: Int) {
        selectedOriginItemId = contractId
        // ...
    }
    fun submitViolation(
        visitDate: String,
        number: Int,
        address: String,
        description: String,
        imageUri: Uri? // برای آپلود عکس در آینده
    ) {
        _submissionState.value = SubmissionState.Loading

        // ساخت آبجکت درخواست با استفاده از ID های ذخیره شده
//        val violationData = ViolationData(
//            organId = selectedOrganId,
//            contractId = selectedContractId,
//            billCleaningViolationGroupId = selectedViolationGroupId,
//            billCleaningViolationId = selectedViolationId,
//            billCleaningItemGroupId = selectedItemGroupId,
//            billOriginCleaningItemId = selectedOriginItemId,
//            visitDate = visitDate,
//            number = number,
//            address = address,
//            visitedFault = description
//        )
        val violationData = ViolationData(
            visitDate = "2025-05-06T00:00:00",
            tenantId = 1,
            number = 111,
            visitedFault = "توضیحات",
            address = "توضیحات",
            billCleaningViolationId = 267,
            contractId = 2273,
            organId = 1291,
            billCleaningViolationGroupId = 81,
            billCleaningItemGroupId = 39,
            billOriginCleaningItemId = 6554
        )

        // اعتبارسنجی: مطمئن شوید تمام ID های لازم انتخاب شده‌اند
        if (violationData.organId == null || violationData.contractId == null || violationData.billCleaningViolationId == null) {
            _submissionState.value = SubmissionState.Error("لطفاً فیلدهای کارفرما، قرارداد و عنوان نقص را پر کنید.")
            return
        }

        viewModelScope.launch {
            try {
                // مرحله ۱: دریافت توکن از SharedPreferences
                val token = prefs.getString("ACCESS_TOKEN", null)
                if (token.isNullOrEmpty()) {
                    _submissionState.value = SubmissionState.Error("توکن دسترسی یافت نشد. لطفاً دوباره وارد شوید.")
                    return@launch
                }

                // مرحله ۲: تبدیل آبجکت درخواست به رشته JSON
                val gson = Gson()
                val modelJson = gson.toJson(SubmitViolationRequest(violationData))
                Log.d("ViewModelLog", "Request JSON: $modelJson") // برای دیباگ

                // مرحله ۳: ساخت MultipartBody.Part
                val modelPart = MultipartBody.Part.createFormData("model", modelJson)

                // مرحله ۴: فراخوانی سرویس Retrofit
                val response = violationApiService.createViolation("Bearer $token", modelPart)

                // مرحله ۵: مدیریت پاسخ سرور
                if (response.isSuccessful && response.body() != null) {
                    // اگر پاسخ موفقیت آمیز بود و شامل ID بود
                    val successBody = response.body()!!
                    val newId = successBody.billOriginEventId.toString()
                    _submissionState.value = SubmissionState.Success("تخلف با موفقیت ثبت شد", newId)

                } else {
                    // اگر پاسخ سرور شامل پیام خطا بود
                    var errorMessage = "خطای نامشخص از سرور"
                    if (response.errorBody() != null) {
                        try {
                            val errorResponse = gson.fromJson(
                                response.errorBody()!!.charStream(),
                                ViolationErrorResponse::class.java
                            )
                            if (!errorResponse.message.isNullOrEmpty()) {
                                errorMessage = errorResponse.message
                            }
                        } catch (e: Exception) {
                            errorMessage = "پاسخ سرور قابل پردازش نیست. کد خطا: ${response.code()}"
                        }
                    }
                    _submissionState.value = SubmissionState.Error(errorMessage)                }

            } catch (e: Exception) {
                // مدیریت خطاهای کلی مانند خطای شبکه
                Log.e("ViewModelLog", "Submission failed", e)
                _submissionState.value = SubmissionState.Error("خطا در ارتباط با سرور: " + e.message)
            }
        }
    }
    // یک کلاس sealed برای مدیریت وضعیت‌ها تعریف کنید
    sealed class SubmissionState {
        object Loading : SubmissionState()
        data class Success(val message: String, val violationId: String) : SubmissionState()
        data class Error(val message: String) : SubmissionState()
    }
} 