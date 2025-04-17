package io.github.kroune.controller

import data.dao.UsersDataServiceImpl
import controller.Bcrypter
import data.UsersDataTable
import data.dao.UsersDataServiceI
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

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
}