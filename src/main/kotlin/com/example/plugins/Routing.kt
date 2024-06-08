package com.example.plugins

import com.example.Users.checkLoginData
import com.example.Users.isLoginPresent
import com.example.Users.login
import com.example.Users.register
import com.example.Users.validateJwtToken
import com.example.game.Connection
import com.example.game.Games
import com.example.game.searching.SearchingForGame
import com.example.jwtToken.CustomJwtToken
import com.kr8ne.mensMorris.move.Movement
import com.kroune.NetworkResponse
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.util.pipeline.*
import io.ktor.utils.io.errors.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.json.Json


fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Hello, world!")
        }
        route("/api/v1/user/") {
            /**
             * @return JWT token or `HttpStatusCode.Conflict` if login is already used
             */
            get("reg") {
                val login = call.parameters["login"]!!
                val password = call.parameters["password"]!!
                if (isLoginPresent(login)) {
                    notify(409, "login is already in use")
                    return@get
                }
                try {
                    register(login, password).getOrThrow()
                } catch (e: IllegalStateException) {
                    notify(401, "server error")
                    println("error registering")
                    e.printStackTrace()
                    return@get
                }
                try {
                    val jwtToken = login(login, password).getOrThrow()
                    notify(200, jwtToken)
                } catch (e: IllegalStateException) {
                    notify(401, "server error")
                    println("error logging in")
                    e.printStackTrace()
                    return@get
                }
            }
            /**
             * @return JWT token
             */
            get("login") {
                val login = call.parameters["login"]!!
                val password = call.parameters["password"]!!
                try {
                    if (!checkLoginData(login, password)) {
                        error("cookie isn't present")
                    }
                    val bdCallResult = login(login, password).getOrThrow()
                    notify(200, bdCallResult)
                } catch (e: IllegalStateException) {
                    notify(401, "server error")
                    println("error logging in")
                    e.printStackTrace()
                    return@get
                }
            }
            get("check-jwt-token") {
                val jwtToken = CustomJwtToken(call.parameters["jwtToken"]!!)
                notify(200, validateJwtToken(jwtToken).toString())
            }
            get("is-playing") {
                val jwtToken = CustomJwtToken(call.parameters["jwtToken"]!!)
                val checkResult = validateJwtToken(jwtToken)
                if (!checkResult) {
                    notify(401, "incorrect jwt token")
                    println("jwt token check failed")
                    return@get
                }
                val gameId = Games.gameId(jwtToken)
                if (gameId.isFailure) {
                    notify(409, "server error")
                    return@get
                } else {
                    notify(200, gameId.getOrThrow().toString())
                }
            }
            webSocket("/search-for-game") {
                val jwtToken = CustomJwtToken(call.parameters["jwtToken"]!!)
                val checkResult = validateJwtToken(jwtToken)
                if (!checkResult) {
                    notify(401, "incorrect jwt token")
                    println("jwt token check failed")
                    close()
                    return@webSocket
                }
                val thisConnection = Connection(jwtToken, this)
                SearchingForGame.addUser(thisConnection)
            }
            webSocket("/game") {
                val jwtToken = CustomJwtToken(call.parameters["jwtToken"]!!)
                val id = call.parameters["gameId"]!!.toLong()
                val game = Games.getGame(id)
                if (game == null) {
                    notify(401, "incorrect game id")
                    println("incorrect game id")
                    close()
                    return@webSocket
                }
                if (!game.isParticipating(jwtToken)) {
                    notify(401, "incorrect jwt token")
                    close()
                    return@webSocket
                }
                try {
                    game.updateSession(jwtToken, this)
                    val isGreen = when (jwtToken) {
                        game.firstUser.jwtToken -> {
                            game.isFirstPlayerGreen
                        }

                        game.secondUser.jwtToken -> {
                            !game.isFirstPlayerGreen
                        }

                        else -> {
                            error("")
                        }
                    }
                    notify(202, isGreen.toString())
                    // we send position to the new connection
                    game.sendPosition(jwtToken, false)
                    incoming.consumeEach { frame ->
                        if (frame !is Frame.Text) return@consumeEach
                        if (!frame.fin) {
                            notify(400, "fin is false")
                            return@consumeEach
                        }
                        val move = try {
                            Json.decodeFromString<Movement>(frame.readText())
                        } catch (e: Exception) {
                            null
                        }
                        if (move == null) {
                            notify(400, "error decoding movement ${frame.readText()}")
                            return@consumeEach
                        }
                        if (!game.isValidMove(move, jwtToken)) {
                            notify(400, "impossible move")
                            return@consumeEach
                        }
                        game.applyMove(move)
                        // send new position to the enemy
                        game.sendMove(jwtToken, move, true)
                        // check if game has ended and then send this info
                        if (game.hasEnded()) {
                            notify(410, "game ended", game.firstUser.session)
                            notify(410, "game ended", game.secondUser.session)
                            close()
                            return@webSocket
                        }
                    }
                } catch (e: IOException) {
                    println("user disconnected")
                }
            }
        }
    }
}

suspend fun PipelineContext<Unit, ApplicationCall>.notify(
    code: Int, message: String = ""
) {
    this.call.notify(code, message)
}

suspend fun ApplicationCall.notify(
    code: Int, message: String = ""
) {
    this.respondText { NetworkResponse(code, message).encode() }
    println(NetworkResponse(code, message).encode())
}

suspend fun DefaultWebSocketSession.notify(
    code: Int, message: String = "", session: DefaultWebSocketSession = this
) {
    session.send(NetworkResponse(code, message).encode())
    println(NetworkResponse(code, message).encode())
}

val SECRET_SERVER_TOKEN = System.getenv("SECRET_SERVER_TOKEN") ?: throw IllegalStateException("missing env variable")
