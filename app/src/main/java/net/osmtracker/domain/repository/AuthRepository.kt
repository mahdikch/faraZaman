package net.osmtracker.domain.repository

import net.osmtracker.data.model.res.LoginResponse

interface AuthRepository {
    suspend fun login(username: String, password: String): LoginResponse
} 