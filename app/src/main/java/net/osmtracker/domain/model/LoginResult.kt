package net.osmtracker.domain.model

import net.osmtracker.data.model.res.LoginResponse

sealed class LoginResult {
    data class Success(val response: LoginResponse) : LoginResult()
    data class Error(val message: String) : LoginResult()
} 