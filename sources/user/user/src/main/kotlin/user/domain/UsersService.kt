package user.domain

import common.JwtTokenImpl
import org.koin.core.component.KoinComponent
import userApi.data.dao.UsersDataServiceI
import userApi.domain.Bcrypter
import userApi.domain.UsersServiceI
import userApi.model.InsertUserPayload
import userApi.model.UserData

class UsersService(
    private val usersDataService: UsersDataServiceI
) : UsersServiceI, KoinComponent {

    override suspend fun getIdByJwtToken(jwtToken: String): Long? {
        return usersDataService.getIdByJwtToken(jwtToken)
    }

    override suspend fun exists(login: String, password: String): Boolean {
        val hash = usersDataService.getPasswordHash(login, password)
        return Bcrypter.verify(
            password,
            hash
        )
    }

    override suspend fun register(insertUserPayload: InsertUserPayload): UsersServiceI.RegistrationResult {
        return runCatching {
            if (usersDataService.isLoginPresent(insertUserPayload.login)) {
                return@runCatching UsersServiceI.RegistrationResult.LoginAlreadyTaken
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
            return@runCatching UsersServiceI.RegistrationResult.Success(jwtToken)
        }.getOrElse {
            UsersServiceI.RegistrationResult.InternalServerError(it)
        }
    }
}