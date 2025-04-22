package common

import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf


suspend inline fun <reified T> RoutingContext.respondSerialized(
    data: T,
    contentType: ContentType? = null,
    status: HttpStatusCode? = null,
    noinline configure: OutgoingContent.() -> Unit = {}
) {
    val text = Json.encodeToString<T>(data)
    call.respondText(text, contentType, status, configure)
}

fun getRandomString(length: Int): String {
    val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
    return (1..length).map { allowedChars.random() }.joinToString("")
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
    val data = incoming.receiveCatching()
    return data.getOrThrow().decodeServerEvent<A, B>()
}

@OptIn(ExperimentalSerializationApi::class)
@PublishedApi
internal inline fun <reified A> ByteArray.decodeProtobuf(): A {
    return ProtoBuf.decodeFromByteArray<A>(this)
}

@OptIn(ExperimentalSerializationApi::class)
@PublishedApi
internal inline fun <reified A> A.encodeProtobuf(): ByteArray {
    return ProtoBuf.encodeToByteArray(this)
}

suspend inline fun <reified A, reified B> WebSocketServerSession.sendSerializedEvent(data: A, metadata: B) {
    send(ServerEvent(data.encodeProtobuf(), metadata.encodeProtobuf()).encodeProtobuf())
}
