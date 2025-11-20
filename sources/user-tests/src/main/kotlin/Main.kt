package io.github.kroune

import org.koin.core.context.GlobalContext
import userApi.domain.UsersServiceI
import userApi.model.InsertUserPayload

suspend fun createDummyUser(
    insertUserPayload: InsertUserPayload = InsertUserPayload("testLogin", "testLogin")
): Pair<InsertUserPayload, String> {
    val koin = GlobalContext.get()
    val usersService by koin.inject<UsersServiceI>()
    val jwt = (usersService.register(insertUserPayload) as UsersServiceI.RegistrationResult.Success).jwtToken
    return insertUserPayload to jwt
}
