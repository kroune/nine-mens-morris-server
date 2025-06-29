package user

import common.JwtTokenImpl
import user.domain.UsersService
import org.koin.core.component.inject


suspend inline fun JwtTokenImpl.verify(): Boolean {
    val login = getLogin().getOrElse {
        return false
    }
    val password = getPassword().getOrElse {
        return false
    }
    val usersService by inject<UsersService>()
    return usersService.exists(login, password)
}