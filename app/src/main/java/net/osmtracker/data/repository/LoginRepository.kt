package net.osmtracker.data.repository

import net.osmtracker.data.api.LoginApi
import net.osmtracker.data.db.dao.UserDao
import net.osmtracker.data.db.model.UserEntity
import net.osmtracker.data.model.req.LoginRequest
import net.osmtracker.data.model.res.LoginResponse
import javax.inject.Inject

class LoginRepository @Inject constructor(
    private val loginApi: LoginApi,
    private val userDao: UserDao
) {
    suspend fun login(username: String, password: String): Result<LoginResponse> {
        return try {
            val response = loginApi.login(LoginRequest(username, password))
            userDao.insertUser(UserEntity(response.userId, response.accessToken))
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUser(userId: Int): UserEntity? {
        return userDao.getUser(userId)
    }
}