package net.osmtracker.presentation.login

import net.osmtracker.domain.model.User

sealed class LoginState {
    object Loading : LoginState()
    data class Success(val user: User) : LoginState()
    data class Error(val message: String) : LoginState()
}