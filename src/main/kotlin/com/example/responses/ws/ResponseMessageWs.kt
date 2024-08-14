package com.example.responses.ws

import io.ktor.server.websocket.*
import io.ktor.websocket.*

/**
 * closes web socket connection
 */
suspend inline fun DefaultWebSocketServerSession.someThingsWentWrong(message: String) {
    close(CloseReason(406_0, "Something went wrong $message"))
}

/**
 * closes web socket connection
 */
suspend inline fun DefaultWebSocketServerSession.jwtTokenIsNotValidForThisGame() {
    close(CloseReason(400_0, "this [jwtToken] isn't valid for this game"))
}

/**
 * closes web socket connection
 */
suspend inline fun DefaultWebSocketServerSession.noGameId() {
    close(CloseReason(400_0, "no [gameId] parameter found"))
}

/**
 * closes web socket connection
 */
suspend inline fun DefaultWebSocketServerSession.gameIdIsNotValid() {
    close(CloseReason(400_0, "[gameId] parameter is not valid"))
}

/**
 * closes web socket connection
 */
suspend inline fun DefaultWebSocketServerSession.gameIdIsNotLong() {
    close(CloseReason(400_0, "[gameId] parameter is not a valid representation of a number"))
}

/**
 * closes web socket connection
 */
suspend inline fun DefaultWebSocketServerSession.noJwtToken() {
    close(CloseReason(400_0, "no [jwtToken] parameter found"))
}

/**
 * closes web socket connection
 */
suspend inline fun DefaultWebSocketServerSession.jwtTokenIsNotValid() {
    close(CloseReason(403_0, "[jwtToken] parameter is not valid"))
}

suspend inline fun DefaultWebSocketServerSession.internalServerError() {
    close(CloseReason(500_0, "Internal server error"))
}