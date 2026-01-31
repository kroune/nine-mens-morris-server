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
package io.github.kroune.server.routing.misc.get

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import java.text.SimpleDateFormat
import java.util.*

internal fun Route.miscRoutingGET() {
    get("/") {
        call.respondText("Hello, world, this is nine mens morris server!")
    }
    get("/healthz") {
        val sdf = SimpleDateFormat("hh:mm:ss dd/M/yyyy")
        val currentDate = sdf.format(Date())
        call.respond(HttpStatusCode.OK, "I am fine at $currentDate")
    }
}
