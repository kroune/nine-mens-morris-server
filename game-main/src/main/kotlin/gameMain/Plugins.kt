package gameMain

import common.ConfigurationLoader.currentConfig
import io.ktor.serialization.kotlinx.*
import io.ktor.server.application.*
import io.ktor.server.websocket.*
import kotlinx.serialization.json.Json

fun Application.applyGamePlugins() {
    install(WebSockets) {
        contentConverter = KotlinxWebsocketSerializationConverter(Json)
        val webSocketConfig = currentConfig.webSocketConfig
        pingPeriod = webSocketConfig.pingPeriod
        timeout = webSocketConfig.timeout
    }
}