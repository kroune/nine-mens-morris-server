package user

import common.JwtTokenImpl
import user.controller.UsersController
import org.koin.core.component.inject


suspend inline fun JwtTokenImpl.verify(): Boolean {
    val login = getLogin().getOrElse {
        return false
    }
    val password = getPassword().getOrElse {
        return false
    }
    val usersController by inject<UsersController>()
    return usersController.exists(login, password)
}