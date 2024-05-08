package com.example.plugins

import at.favre.lib.crypto.bcrypt.BCrypt
import com.example.GameData
import com.example.MovementAdapter
import com.example.toMovement
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import java.util.*
import java.util.concurrent.atomic.AtomicLong

val usersDB = Users()
val games: MutableMap<Long, Game> = Collections.synchronizedMap(mutableMapOf<Long, Game>())
val usersSearchingForGame: Queue<Connection> = LinkedList()
val atomicGameId = AtomicLong(0)
const val cost = 20
fun Application.configureRouting() {
    routing {
        route("/api/v1/user/") {
            get("reg-{login}-{password}") {
                val login = call.parameters["login"]!!
                val password = call.parameters["password"]!!
                usersDB.register(login, password)
            }
            get("log-{login}-{password}") {
                val login = call.parameters["login"]!!
                val password = call.parameters["password"]!!
                when (val bdCall = usersDB.login(login, password)) {
                    is LoginResult.FAIL -> {
                        call.respondText { "incorrect ${bdCall.string}" }
                    }

                    is LoginResult.SUCCESS -> {
                        call.respondText { bdCall.string }
                    }
                }
            }
            webSocket("/search-for-game") {
                val cookie = this.incoming.receive().data.toString()
                val checkResult = usersDB.checkCookie(cookie)
                if (!checkResult) {
                    call.respondText { "incorrect cookie" }
                }
                val thisConnection = Connection(cookie, this)
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
                close(CloseReason(400, "t"))
            }
        }
    }
}

class Users {
    private val users = Collections.synchronizedList<User>(mutableListOf())
    fun checkCookie(cookie: String): Boolean {
        return users.any { it.cookie == cookie }
    }

    fun register(login: String, password: String) {
        val stringToHash = "$login~$password"
        val hash = BCrypt.with(BCrypt.Version.VERSION_2B).hashToChar(cost, stringToHash.toCharArray()).toString()
        users.add(User(hash))
    }

    fun login(login: String, password: String): LoginResult {
        val stringToHash = "$login~$password"
        val hash = BCrypt.with(BCrypt.Version.VERSION_2B).hashToChar(cost, stringToHash.toCharArray()).toString()
        val cookie = users.find { it.cookie == hash }?.cookie
        if (cookie == null) {
            return LoginResult.FAIL("cookie isn't present")
        }
        return LoginResult.SUCCESS(cookie)
    }
}

sealed class LoginResult(val string: String) {
    class SUCCESS(cookie: String) : LoginResult(cookie)
    class FAIL(error: String) : LoginResult(error)
}

class User(val cookie: String)

//{"startIndex":null,"endIndex":5}
class Connection(var cookie: String, val session: DefaultWebSocketSession)

class Game(val firstUser: Connection, val secondUser: Connection) {
    var id: Long = atomicGameId.incrementAndGet()
    val gamePos = GameData()
}