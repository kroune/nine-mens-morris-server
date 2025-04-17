package routing

import com.kroune.nineMensMorrisLib.move.Movement
import com.kroune.nineMensMorrisShared.GameEndReason
import io.github.kroune.controller.GameController
import io.github.kroune.controller.UsersController
import io.github.kroune.logging.gameId
import io.github.kroune.logging.logger
import io.github.kroune.logging.userId
import io.github.kroune.routing.jwtTokenIsNotValidForThisGame
import io.github.kroune.routing.requireValidJwtToken
import io.github.kroune.routing.someThingsWentWrong
import io.ktor.server.routing.Route
import io.ktor.server.websocket.sendSerialized
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.serialization.json.Json
import org.koin.ktor.ext.inject


fun Route.gameMainRouting() {
    webSocket("/game") {
        val jwtToken = requireValidJwtToken() ?: return@webSocket
        val gameId = requireGameId() ?:return@webSocket

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
            val isGreen = if (game.isFirstPlayer(userId)) {
                game.isFirstPlayerMovesFirst()
            } else {
                !game.isFirstPlayerMovesFirst()
            }
            val enemyId = game.enemyId(userId)
            sendSerialized(isGreen)
            logger.atDebug {
                message = "sending isGreen info - [$isGreen]"
                payload = buildMap {
                    userId(userId)
                    gameId(gameId)
                }
            }
            sendSerialized(enemyId)
            logger.atDebug {
                message = "sending enemy id info - [$enemyId]"
                payload = buildMap {
                    userId(userId)
                    gameId(gameId)
                }
            }
            // we send position to the new connections
            game.sendPosition(this)
            logger.atDebug {
                message = "sending position info"
                payload = buildMap {
                    userId(userId)
                    gameId(gameId)
                }
            }
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
