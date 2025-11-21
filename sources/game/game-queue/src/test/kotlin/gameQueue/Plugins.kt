package gameQueue

import common.ConfigurationLoader.currentConfig
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import kotlinx.serialization.json.Json

fun Application.applyGamePlugins() {
    install(WebSockets) {
        contentConverter = KotlinxWebsocketSerializationConverter(Json)
        val webSocketConfig = currentConfig.webSocketConfig
        pingPeriod = webSocketConfig.pingPeriod
        timeout = webSocketConfig.timeout
    }
}