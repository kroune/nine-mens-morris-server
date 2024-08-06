package com.example

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.websocket.*
import io.ktor.util.pipeline.*
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

suspend fun PipelineContext<Unit, ApplicationCall>.noJwtToken() {
    call.respond(HttpStatusCode.BadRequest, "no [jwtToken] parameter found")
}

suspend inline fun PipelineContext<Unit, ApplicationCall>.jwtTokenIsNotValid() {
    call.respond(HttpStatusCode.Forbidden, "[jwtToken] parameter is not valid")
}

/**
 * closes web socket connection
 */
suspend inline fun DefaultWebSocketServerSession.jwtTokenIsNotValid() {
    close(CloseReason(403_0, "[jwtToken] parameter is not valid"))
}

suspend fun PipelineContext<Unit, ApplicationCall>.noLogin() {
    call.respond(HttpStatusCode.BadRequest, "no [login] parameter found")
}

suspend fun PipelineContext<Unit, ApplicationCall>.noValidLogin() {
    call.respond(HttpStatusCode.BadRequest, "no [login] parameter found")
}

suspend fun PipelineContext<Unit, ApplicationCall>.noPassword() {
    call.respond(HttpStatusCode.BadRequest, "no [password] parameter found")
}

suspend fun PipelineContext<Unit, ApplicationCall>.noUserId() {
    call.respond(HttpStatusCode.BadRequest, "no [id] parameter found")
}

suspend fun PipelineContext<Unit, ApplicationCall>.userIdIsNotLong() {
    call.respond(HttpStatusCode.Forbidden, "[id] parameter is not a long")
}

suspend fun PipelineContext<Unit, ApplicationCall>.userIdIsNotValid() {
    call.respond(HttpStatusCode.Forbidden, "[id] parameter is not valid")
}

suspend inline fun PipelineContext<Unit, ApplicationCall>.imageIsNotValid() {
    call.respond(HttpStatusCode.Forbidden, "provided image (byte array) is not valid")
}