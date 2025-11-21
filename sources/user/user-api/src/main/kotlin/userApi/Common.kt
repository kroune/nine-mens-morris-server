package userApi

import common.JwtTokenImpl
import org.koin.core.component.inject
import userApi.domain.UsersServiceI

suspend inline fun JwtTokenImpl.verify(): Boolean {
    val login = getLogin().getOrElse {
        return false
    }
    val password = getPassword().getOrElse {
        return false
    }
    val usersService by inject<UsersServiceI>()
    return usersService.exists(login, password)
}