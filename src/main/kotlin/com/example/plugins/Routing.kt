package com.example.plugins

import com.example.Users
import com.example.game.GameData
import com.example.game.Games
import com.example.game.MovementAdapter
import com.example.game.toMovement
import io.ktor.http.*
import io.ktor.network.sockets.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import java.net.SocketException
import java.util.*

val usersSearchingForGame: Queue<com.example.game.Connection> = LinkedList()

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
                println("operation start")
                println("login - $login")
                println("password - $password")
                try {
                    Users.register(login, password)
                } catch (e: IllegalStateException) {
                    call.respond(HttpStatusCode.Conflict)
                    println("error registering")
                    e.printStackTrace()
                    return@get
                }
                try {
                    val bdCall = Users.login(login, password)
                    println(bdCall)
                    call.respondText(bdCall)
                } catch (e: IllegalStateException) {
                    call.respond(HttpStatusCode.Unauthorized)
                    println("error logging in")
                    e.printStackTrace()
                    return@get
                }
                println("operation end")
            }
            /**
             * @return JWT token
             */
            get("login") {
                val login = call.parameters["login"]!!
                val password = call.parameters["password"]!!
                println("operation start")
                println("login - $login")
                println("password - $password")
                try {
                    val bdCall = Users.login(login, password)
                    println(bdCall)
                    call.respondText(bdCall)
                } catch (e: IllegalStateException) {
                    call.respond(HttpStatusCode.Unauthorized)
                    println("error logging in")
                    e.printStackTrace()
                    return@get
                }
                println("operation end")
            }
            get("is-playing") {
                val jwtToken = call.parameters["jwtToken"]!!
                val checkResult = Users.checkJWTToken(jwtToken)
                println()
                if (!checkResult) {
                    call.respondText { "incorrect jwt token" }
                    println("jwt token check failed")
                    return@get
                }
                call.respondText { Games.gameId(jwtToken).toString() }
            }
            webSocket("/search-for-game") {
                val jwtToken = call.parameters["jwtToken"]!!
                val checkResult = Users.checkJWTToken(jwtToken)
                if (!checkResult) {
                    call.respondText { "incorrect jwt token" }
                    println("jwt token check failed")
                    close()
                    return@webSocket
                }
                if (usersSearchingForGame.any { it.jwtToken == jwtToken }) {
                    call.respondText { "you are already searching for game" }
                    println("already searching for game")
                    close()
                    return@webSocket
                }
                val thisConnection = com.example.game.Connection(jwtToken, this)
                usersSearchingForGame.add(thisConnection)
                println("new connection")
                try {
                    while (usersSearchingForGame.size < 2) {
                        delay(3000L)
                    }
                    println("found enemy")
                    val enemy = usersSearchingForGame.first { it.jwtToken != thisConnection.jwtToken }
                    val id = Games.createGame(thisConnection, enemy)
                    // send game id to 2 players
                    thisConnection.session.send(id.toString())
                    enemy.session.send(id.toString())
                    close()
                } catch (e: SocketException) {
                    println("remove connection")
                    usersSearchingForGame.remove(thisConnection)
                }
            }
            webSocket("/game") {
                val jwtToken = call.parameters["jwtToken"]!!
                val id = call.parameters["gameId"]!!.toLong()
                val game = Games.getGame(id)
                if (game == null) {
                    call.respondText { "incorrect game id" }
                    close()
                    return@webSocket
                }
                if (!game.isValidJwtToken(jwtToken)) {
                    call.respondText { "incorrect jwt token" }
                    close()
                    return@webSocket
                }
                println("game accessed")
                incoming.consumeEach { frame ->
                    if (frame !is Frame.Text) return@consumeEach
                    val move = Json.decodeFromString<MovementAdapter>(frame.readText()).toMovement()
                    println("new move")
                    game.applyMove(move)
                    game.notifyAboutChanges()
                }
            }
        }
    }
}

val SECRET = "secretToken"
