package com.example.plugins

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

val users = Collections.synchronizedSet(mutableSetOf<Connection>())
val games = Collections.synchronizedMap(mutableMapOf<Long, Game>())
val atomicUserId = AtomicLong(0)
val atomicGameId = AtomicLong(0)
fun Application.configureRouting() {
    routing() {
        get("/hello") {
            call.respondText("Hello World!")
        }
        route("/api/v1/user/") {
            webSocket("/start-searching-game") {
                val thisConnection = Connection(session = this)
                users.add(thisConnection)
                while (users.size < 2) {
                    delay(5000L)
                }
                val enemy = users.first { it.id != thisConnection.id }
                val gameData = Game(thisConnection, enemy)
                games[gameData.id] = gameData
                thisConnection.session.send(gameData.id.toString())
                enemy.session.send(gameData.id.toString())
            }
            webSocket("/game-{id}") {
                val id = call.parameters["id"]?.toLong()
                val game = games[id]!!
                incoming.consumeEach { frame ->
                    if (frame !is Frame.Text)
                        return@consumeEach
                    val move = Json.decodeFromString<MovementAdapter>(frame.readText()).toMovement()
                    game.firstUser.session.send("2")
                    game.secondUser.session.send("1")
//                    game.gamePos.applyMove(move)
//                    game.firstUser.session.send(game.gamePos.getPosition())
//                    game.secondUser.session.send(game.gamePos.getPosition())
                }
                close(CloseReason(400, "wtf"))
            }
        }
    }
}

//{"startIndex":null,"endIndex":5}
class Connection(var id: Long = atomicUserId.incrementAndGet(), val session: DefaultWebSocketSession) {
}

class Game(val firstUser: Connection, val secondUser: Connection) {
    var id: Long = atomicGameId.incrementAndGet()
    val gamePos = GameData()
}
