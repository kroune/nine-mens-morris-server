package common

import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.server.websocket.WebSocketServerSession
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.send
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

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

suspend fun DefaultWebSocketServerSession.closeWithTimeout(
    reason: CloseReason = CloseReason(CloseReason.Codes.NORMAL, ""),
    timeout: Duration = 20.seconds
) {
    withTimeoutOrNull(timeout) {
        this@closeWithTimeout.close(reason)
    } ?: this@closeWithTimeout.cancel()
}