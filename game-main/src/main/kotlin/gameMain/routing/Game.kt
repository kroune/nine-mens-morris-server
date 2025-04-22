package gameMain.routing

import com.kroune.nineMensMorrisLib.move.Movement
import com.kroune.nineMensMorrisShared.GameEndReason
import gameMain.Game
import gameMain.GameDataFactory
import gameMain.controller.GameController
import user.controller.UsersController
import common.logging.gameId
import common.logging.globalLogger
import common.logging.logger
import common.logging.userId
import user.routing.jwtTokenIsNotValidForThisGame
import user.routing.requireValidJwtToken
import user.routing.someThingsWentWrong
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.serialization.json.Json
import org.koin.ktor.ext.inject


fun Route.gameMainRouting() {
    webSocket("/game") {
        val jwtToken = requireValidJwtToken() ?: return@webSocket
        val gameId = requireGameId() ?: return@webSocket

        val gameController by inject<GameController>()

        val usersController by inject<UsersController>()
        val userId = usersController.getIdByJwtToken(jwtToken)!!

        if (!gameController.participates(userId)) {
            jwtTokenIsNotValidForThisGame()
            return@webSocket
        }
        val game = GameDataFactory.getGame(gameId)
        try {
            val isFirstUser = game.isFirstPlayer(userId)
            sendInitialInfo(game, userId)
            game.updateSession(userId, this)
            while (true) {
                val frame = this.incoming.receive()
                if (frame !is Frame.Text) continue
                val move = try {
                    Json.decodeFromString<Movement>(frame.readText())
                } catch (e: Exception) {
                    logger.atDebug {
                        message = "error decoding client movement: frameType - [${frame.frameType}]"
                        payload = buildMap {
                            userId(userId)
                            gameId(gameId)
                        }
                        cause = e
                    }
                    someThingsWentWrong("error decoding client movement")
                    return@webSocket
                }
                // user gave up
                if (move.startIndex == null && move.endIndex == null) {
                    logger.atDebug {
                        message = "user gave up"
                        payload = buildMap {
                            userId(userId)
                            gameId(gameId)
                        }
                    }
                    game.handleGameEnd(GameEndReason.UserGaveUp(isFirstUser))
                    return@webSocket
                }
                if (!game.isMovePossible(move, userId)) {
                    logger.atDebug {
                        message = "received an illegal move - [$move]"
                        payload = buildMap {
                            userId(userId)
                            gameId(gameId)
                        }
                    }
                    someThingsWentWrong("received an illegal move")
                    return@webSocket
                }
                // send new position to the enemy
                game.sendMove(userId, move, true)
                game.applyMove(move, isFirstUser)
                // note: checking if the game has ended happens in [GameData.applyMove]
            }
        } catch (e: ClosedReceiveChannelException) {
            // this exception is thrown if websocket session was closed, and we tried to receive smth
            logger.atDebug {
                message = "channel was closed"
                payload = buildMap {
                    userId(userId)
                    gameId(gameId)
                }
                cause = e
            }
            return@webSocket
        } catch (e: Exception) {
            logger.atError {
                message = "uncaught exception"
                payload = buildMap {
                    userId(userId)
                    gameId(gameId)
                }
                cause = e
            }
        }
    }
}

suspend fun DefaultWebSocketServerSession.sendInitialInfo(game: Game, userId: Long) {
    val isGreen = if (game.isFirstPlayer(userId)) {
        game.isFirstPlayerMovesFirst()
    } else {
        !game.isFirstPlayerMovesFirst()
    }
    val gameId = game.gameId
    val enemyId = game.enemyId(userId)
    sendSerialized(isGreen)
    globalLogger.atDebug {
        message = "sending isGreen info - [$isGreen]"
        payload = buildMap {
            userId(userId)
            gameId(gameId)
        }
    }
    sendSerialized(enemyId)
    globalLogger.atDebug {
        message = "sending enemy id info - [$enemyId]"
        payload = buildMap {
            userId(userId)
            gameId(gameId)
        }
    }
    // we send position to the new connections
    game.sendPosition(this)
    globalLogger.atDebug {
        message = "sending position info"
        payload = buildMap {
            userId(userId)
            gameId(gameId)
        }
    }
}