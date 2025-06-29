package io.github.kroune

import org.koin.core.context.GlobalContext
import user.domain.UsersService
import user.model.InsertUserPayload

suspend fun createDummyUser(
    insertUserPayload: InsertUserPayload = InsertUserPayload("testLogin", "testLogin")
): Pair<InsertUserPayload, String> {
    val koin = GlobalContext.get()
    val usersService by koin.inject<UsersService>()
    val jwt = (usersService.register(insertUserPayload) as UsersService.RegistrationResult.Success).jwtToken
    return insertUserPayload to jwt
}
