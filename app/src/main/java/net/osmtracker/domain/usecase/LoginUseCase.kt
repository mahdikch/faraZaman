package net.osmtracker.domain.usecase

import net.osmtracker.data.repository.LoginRepository
import net.osmtracker.domain.model.User
//import javax.inject.Inject

//class LoginUseCase @Inject constructor(private val repository: LoginRepository) {
//    suspend operator fun invoke(username: String, password: String): Result<User> {
//        return repository.login(username, password).map { response ->
//            User(response.userId, response.accessToken)
//        }
//    }
//}