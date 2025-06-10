package net.osmtracker.presentation.login

import net.osmtracker.data.model.res.LoginResponse

sealed class LoginState {
    object Loading : LoginState()
    data class Success(val response: LoginResponse) : LoginState()
    data class Error(val message: String) : LoginState()
}