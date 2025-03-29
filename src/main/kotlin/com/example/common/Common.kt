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
package com.example.common

import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.server.websocket.WebSocketServerSession
import io.ktor.websocket.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf

@OptIn(ExperimentalSerializationApi::class)
val json = Json {
    prettyPrint = true
    prettyPrintIndent = " "
}


@Serializable
data class ServerEvent(
    val data: ByteArray,
    val metadata: ByteArray
)

inline fun <reified A, reified B> Frame.decodeServerEvent(): Pair<A, B> {
    return data.decodeProtobuf<ServerEvent>().let { (data, metadata) ->
        data.decodeProtobuf<A>() to metadata.decodeProtobuf<B>()
    }
}

suspend inline fun <reified A, reified B> DefaultClientWebSocketSession.receiveDeserializedServerEvent(): Pair<A, B> {
    return this.incoming.receive().decodeServerEvent<A, B>()
}

@PublishedApi
internal inline fun <reified A> ByteArray.decodeProtobuf(): A {
    return ProtoBuf.decodeFromByteArray(this)
}

@PublishedApi
internal inline fun <reified A> A.encodeProtobuf(): ByteArray {
    return ProtoBuf.encodeToByteArray(this)
}

suspend inline fun <reified A, reified B> WebSocketServerSession.sendSerializedEvent(data: A, metadata: B) {
    send(ServerEvent(data.encodeProtobuf(), metadata.encodeProtobuf()).encodeProtobuf())
}