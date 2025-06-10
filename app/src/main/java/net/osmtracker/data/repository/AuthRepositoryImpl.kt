package net.osmtracker.data.repository

import net.osmtracker.data.model.res.LoginResponse
import net.osmtracker.domain.repository.AuthRepository
import net.osmtracker.service.remote.AuthService
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val authService: AuthService
) : AuthRepository {
    override suspend fun login(username: String, password: String): LoginResponse {
        val clientId = "b1aa497a-8778-4351-8c91-9719e2b0362f"
        val clientSecret = "i4uCgAvIAw7BuAn7dDxDW0jHznYaqIhA"
        val authHeader = "Basic " + android.util.Base64.encodeToString(
            "$clientId:$clientSecret".toByteArray(), android.util.Base64.NO_WRAP
        )
        
        val response = authService.login(authHeader, username, password)
        return LoginResponse(
            access_token = response.access_token,
            token_type = response.token_type,
            expires_in = response.expires_in,
            refresh_token = response.refresh_token
        )
    }
} 