// در فایل ViolationModels.kt

package net.osmtracker.data.model

import com.google.gson.annotations.SerializedName

// مدل برای بدنه اصلی درخواست
data class SubmitViolationRequest(
    val request: ViolationData
)

// مدل جدید برای پاسخ موفقیت‌آمیز
data class ViolationSuccessResponse(
    val billOriginEventId: Long
)

// مدلی که قبلاً داشتیم را برای مدیریت پیام‌های خطا استفاده می‌کنیم
data class ViolationErrorResponse(
    @SerializedName("Message")
    val message: String?
)
// مدل برای محتوای داخلی درخواست
data class ViolationData(
    @SerializedName("tenantId")
    val tenantId: Int = 1, // این مقدار معمولاً ثابت است
    @SerializedName("organId")
    val organId: Int?,
    @SerializedName("billCleaningViolationId")
    val billCleaningViolationId: Int?,
    @SerializedName("contractId")
    val contractId: Int?,
    @SerializedName("billCleaningViolationGroupId")
    val billCleaningViolationGroupId: Int?,
    @SerializedName("billCleaningItemGroupId")
    val billCleaningItemGroupId: Int?,
    @SerializedName("billOriginCleaningItemId")
    val billOriginCleaningItemId: Int?,
    @SerializedName("visitDate")
    val visitDate: String?,
    @SerializedName("Number")
    val number: Int?,
    @SerializedName("Address")
    val address: String?,
    @SerializedName("VisitedFault")
    val visitedFault: String? // توضیحات
)

// مدل برای پاسخ سرور
data class SubmitViolationResponse(
    @SerializedName("Data")
    val data: ResponseData?,
    @SerializedName("Message")
    val message: String?,
    @SerializedName("HelpLink")
    val helpLink: String?
)

data class ResponseData(
    @SerializedName("id")
    val id: String
)