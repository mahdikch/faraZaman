package net.osmtracker.domain.usecase

import net.osmtracker.domain.model.LoginResult
import net.osmtracker.domain.repository.AuthRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(username: String, password: String): LoginResult {
        return try {
            val response = authRepository.login(username, password)
            LoginResult.Success(response)
        } catch (e: Exception) {
            LoginResult.Error(e.message ?: "Login failed")
        }
    }
}