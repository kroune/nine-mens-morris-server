package io.github.kroune

import commonTests.TestDatabase
import user.data.InsertUserPayload
import user.controller.UsersController
import io.ktor.http.isSuccess
import kotlinx.coroutines.runBlocking
import org.koin.core.component.inject

fun TestDatabase.createDummyUser(
    insertUserPayload: InsertUserPayload = InsertUserPayload("testLogin", "testLogin")
): Pair<InsertUserPayload, String> {
    val (status, jwt) = runBlocking {
        val usersController by inject<UsersController>()
        usersController.register(insertUserPayload)
    }
    assert(status.isSuccess())
    return insertUserPayload to jwt!!
}