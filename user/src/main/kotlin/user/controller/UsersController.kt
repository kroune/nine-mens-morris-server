package user.controller

import common.JwtTokenImpl
import io.ktor.http.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import user.data.InsertUserPayload
import user.data.UserData
import user.data.dao.UsersDataServiceI

class UsersController: KoinComponent {
    val usersDataService by inject<UsersDataServiceI>()

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

    suspend fun register(insertUserPayload: InsertUserPayload): Pair<HttpStatusCode, String?> {
        if (usersDataService.isLoginPresent(insertUserPayload.login)) {
            return HttpStatusCode.Conflict.description("login is already in use") to null
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
        return HttpStatusCode.OK to jwtToken
    }
}