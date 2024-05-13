package com.example.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.example.GameData
import com.example.MovementAdapter
import com.example.Users
import com.example.toMovement
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import java.time.Duration
import java.util.*
import java.util.concurrent.atomic.AtomicLong

val games: MutableMap<Long, Game> = Collections.synchronizedMap(mutableMapOf<Long, Game>())
val usersSearchingForGame: Queue<Connection> = LinkedList()
val atomicGameId = AtomicLong(0)

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Hello, world1!")
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
                    call.respond(HttpStatusCode.OK, bdCall)
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
                    call.respond(HttpStatusCode.OK, bdCall)
                } catch (e: IllegalStateException) {
                    call.respond(HttpStatusCode.Unauthorized)
                    println("error logging in")
                    e.printStackTrace()
                    return@get
                }
                println("operation end")
            }
            webSocket("/search-for-game") {
                val jwtToken = this.incoming.receive().data.toString()
                val checkResult = Users.checkJWTToken(jwtToken)
                if (!checkResult) {
                    call.respondText { "incorrect cookie" }
                }
                val thisConnection = Connection(jwtToken, this)
                usersSearchingForGame.add(thisConnection)
                while (usersSearchingForGame.size < 2) {
                    delay(5000L)
                }
                val enemy = usersSearchingForGame.first { it.cookie != thisConnection.cookie }
                val gameData = Game(thisConnection, enemy)
                games[gameData.id] = gameData
                // send game id to 2 players
                thisConnection.session.send(gameData.id.toString())
                enemy.session.send(gameData.id.toString())
            }
            webSocket("/game-{id}") {
                val id = call.parameters["id"]?.toLong()
                val game = games[id]!!
                incoming.consumeEach { frame ->
                    if (frame !is Frame.Text) return@consumeEach
                    val move = Json.decodeFromString<MovementAdapter>(frame.readText()).toMovement()
                    game.gamePos.applyMove(move)
                    game.firstUser.session.send(game.gamePos.getPosition())
                    game.secondUser.session.send(game.gamePos.getPosition())
                }
            }
        }
    }
}

val SECRET = "secret"

//{"startIndex":null,"endIndex":5}
class Connection(var cookie: String, val session: DefaultWebSocketSession)

class Game(val firstUser: Connection, val secondUser: Connection) {
    var id: Long = atomicGameId.incrementAndGet()
    val gamePos = GameData()
}