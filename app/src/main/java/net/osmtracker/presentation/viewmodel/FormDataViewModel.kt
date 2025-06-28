package net.osmtracker.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import net.osmtracker.data.model.DropdownItem
import net.osmtracker.data.model.FormDataRequest
import net.osmtracker.data.model.FormDataResponse
import net.osmtracker.data.repository.FormDataRepository
import retrofit2.Response
import javax.inject.Inject

@HiltViewModel
class FormDataViewModel @Inject constructor(
    private val repository: FormDataRepository
) : ViewModel() {
    
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
} 