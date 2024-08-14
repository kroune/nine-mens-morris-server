package com.example.responses.get

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.util.pipeline.*

suspend fun PipelineContext<Unit, ApplicationCall>.noJwtToken() {
    call.respond(HttpStatusCode.BadRequest, "no [jwtToken] parameter found")
}

suspend inline fun PipelineContext<Unit, ApplicationCall>.jwtTokenIsNotValid() {
    call.respond(HttpStatusCode.Forbidden, "[jwtToken] parameter is not valid")
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

suspend inline fun PipelineContext<Unit, ApplicationCall>.internalServerError() {
    call.respond(HttpStatusCode.InternalServerError, "Internal server error")
}
