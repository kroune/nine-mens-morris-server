package user.domain

import common.JwtTokenImpl
import org.koin.core.component.KoinComponent
import user.model.InsertUserPayload
import user.model.UserData
import user.data.dao.UsersDataServiceI

class UsersService(
    private val usersDataService: UsersDataServiceI
) : KoinComponent {

    suspend fun getIdByJwtToken(jwtToken: String): Long? {
        return usersDataService.getIdByJwtToken(jwtToken)
    }

    suspend fun exists(login: String, password: String): Boolean {
        val hash = usersDataService.getPasswordHash(login, password)
        return Bcrypter.verify(
            password,
            hash
        )
    }

    suspend fun register(insertUserPayload: InsertUserPayload): RegistrationResult {
        return runCatching {
            if (usersDataService.isLoginPresent(insertUserPayload.login)) {
                return@runCatching RegistrationResult.LoginAlreadyTaken
            }
            val passwordHash = Bcrypter.hash(insertUserPayload.password)
            usersDataService.create(
                UserData(
                    insertUserPayload.login,
                    passwordHash,
                    insertUserPayload.date,
                    insertUserPayload.rating,
                    insertUserPayload.profilePicture
                )
            )
            val jwtToken = JwtTokenImpl(insertUserPayload.login, insertUserPayload.password).token
            return@runCatching RegistrationResult.Success(jwtToken)
        }.getOrElse {
            RegistrationResult.InternalServerError(it)
        }
    }

    sealed interface RegistrationResult {
        class Success(val jwtToken: String) : RegistrationResult
        object LoginAlreadyTaken : RegistrationResult
        class InternalServerError(val throwable: Throwable) : RegistrationResult
    }
}