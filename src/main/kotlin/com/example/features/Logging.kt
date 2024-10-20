/*
 * This file is part of nine-mens-morris-server (https://github.com/kroune/nine-mens-morris-server)
 * Copyright (C) 2024-2024  kroune
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Contact: kr0ne@tuta.io
 */
package com.example.features

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import io.netty.handler.codec.http.HttpVersion
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.*

//fun PipelineContext<Unit, ApplicationCall>.log(text: String, logLevel: LogPriority = LogPriority.Info) {
//    (this.call as RoutingApplicationCall).log(text, logLevel)
//}

fun log(text: String, logLevel: LogPriority = LogPriority.Info) {
    val shouldPrint = logLevel >= currentLogLevel
    if (!shouldPrint) {
        return
    }
    val sdf = SimpleDateFormat("hh:mm:ss dd/M/yyyy ")
    val currentDate = sdf.format(Date())
//    val loggingInfo = LoggingInfo(
//        this.route,
//        this.request.httpMethod,
//        this.request.headers,
//        this.request.httpVersion,
//        currentConfig,
//        this.request.queryParameters
//    )
    println("$currentDate $text")
}

data class LoggingInfo(
    val route: Route,
    val method: HttpMethod,
    val headers: Headers,
    val httpVersion: String,
    val config: Config,
    val parameters: Parameters
)

val currentLogLevel = currentConfig.currentLogPriority

@Serializable
sealed class LogPriority(private val priority: Int) : Comparable<LogPriority> {
    @Serializable
    data object Errors : LogPriority(5)

    @Serializable
    data object Info : LogPriority(1)

    @Serializable
    data object Debug : LogPriority(0)

    override fun compareTo(other: LogPriority): Int {
        return this.priority - other.priority
    }
}
