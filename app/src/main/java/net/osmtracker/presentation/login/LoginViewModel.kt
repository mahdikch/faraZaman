package net.osmtracker.presentation.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
//import net.osmtracker.domain.usecase.LoginUseCase
//import javax.inject.Inject

//class LoginViewModel @Inject constructor(
//    private val loginUseCase: LoginUseCase
//) : ViewModel() {
//    private val _loginState = MutableLiveData<LoginState>()
//    val loginState: LiveData<LoginState> get() = _loginState
//
//    fun login(username: String, password: String) {
//        _loginState.value = LoginState.Loading
//        viewModelScope.launch {
//            loginUseCase(username, password).onSuccess { user ->
//                _loginState.value = LoginState.Success(user)
//            }.onFailure { error ->
//                _loginState.value = LoginState.Error(error.message ?: "Login failed")
//            }
//        }
//    }
//}