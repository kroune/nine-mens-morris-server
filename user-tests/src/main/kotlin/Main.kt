package io.github.kroune

import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.koin.core.context.GlobalContext
import user.controller.UsersController
import user.data.InsertUserPayload

fun createDummyUser(
    insertUserPayload: InsertUserPayload = InsertUserPayload("testLogin", "testLogin")
): Pair<InsertUserPayload, String> {
    val koin = GlobalContext.get()
    val (status, jwt) = runBlocking {
        val usersController by koin.inject<UsersController>()
        usersController.register(insertUserPayload)
    }
    assert(status.isSuccess())
    return insertUserPayload to jwt!!
}