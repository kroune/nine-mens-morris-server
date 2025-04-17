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
package io.github.kroune.logging

import io.github.oshai.kotlinlogging.KLogger
import io.ktor.server.application.Application
import io.ktor.server.routing.Route
import io.ktor.server.routing.Routing
import org.koin.core.context.GlobalContext
import org.koin.ktor.ext.inject

fun MutableMap<String, Any?>.userId(userId: Long?) {
    if (userId != null) {
        put("userId", userId)
    }
}

fun MutableMap<String, Any?>.gameId(gameId: Long?) {
    if (gameId != null) {
        put("userId", gameId)
    }
}

fun MutableMap<String, Any?>.bucketId(bucketId: Int?) {
    if (bucketId != null) {
        put("userId", bucketId)
    }
}

val globalLogger: KLogger
    get() = GlobalContext.get().inject<KLogger>().value

val Routing.logger: KLogger
    get() = inject<KLogger>().value

val Route.logger: KLogger
    get() = inject<KLogger>().value

val Application.logger: KLogger
    get() = inject<KLogger>().value