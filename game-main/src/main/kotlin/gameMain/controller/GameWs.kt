package gameMain.controller

import com.kroune.nineMensMorrisLib.move.Movement
import com.kroune.nineMensMorrisShared.GameEndReason
import common.logging.gameId
import common.logging.globalLogger
import common.logging.logger
import common.logging.userId
import gameMain.Game
import gameMain.data.dao.GamesDataServiceI
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.serialization.json.Json
import org.koin.ktor.ext.inject
import user.view.jwtTokenIsNotValidForThisGame
import user.controller.requireValidJwtToken
import user.view.someThingsWentWrong
import user.domain.UsersService


fun Route.gameMainRouting() {
    webSocket("/game") {
        val jwtToken = requireValidJwtToken() ?: return@webSocket
        val gameId = requireGameId() ?: return@webSocket

        val usersService by inject<UsersService>()
        val userId = usersService.getIdByJwtToken(jwtToken)!!

        val gamesDataService by inject<GamesDataServiceI>()
        if (!gamesDataService.participates(userId)) {
            jwtTokenIsNotValidForThisGame()
            return@webSocket
        }
        val game = Game(gameId)
        game.initializeGame()
        try {
            val isFirstUser = game.isFirstPlayer(userId)
            sendInitialInfo(game, userId)
            game.updateSession(userId, this)
            while (true) {
                val frame = this.incoming.receive()
                if (frame !is Frame.Text) continue
                val movement = try {
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
                if (movement.startIndex == null && movement.endIndex == null) {
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
                if (!game.isMovePossible(movement, userId)) {
                    logger.atDebug {
                        message = "received an illegal move - [$movement]"
                        payload = buildMap {
                            userId(userId)
                            gameId(gameId)
                        }
                    }
                    someThingsWentWrong("received an illegal move")
                    return@webSocket
                }
                // send new position to the enemy
                game.sendMove(userId, movement, true)
                game.applyMove(movement, isFirstUser)
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