package user

import common.ConfigurationLoader.currentConfig
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.plugins.ratelimit.RateLimitName
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds

fun Application.applyUserPlugins() {
    install(RateLimit) {
        global {
            val rateLimitConfig = currentConfig.rateLimitConfig
            rateLimiter(
                limit = rateLimitConfig.rateLimit,
                refillPeriod = rateLimitConfig.refillSpeed,
                initialSize = rateLimitConfig.initialSize
            )
        }
        register(RateLimitName("imageUploading")) {
            rateLimiter(limit = 3, refillPeriod = 60.seconds, initialSize = 5)
        }
    }
    install(WebSockets) {
        contentConverter = KotlinxWebsocketSerializationConverter(Json)
        val webSocketConfig = currentConfig.webSocketConfig
        pingPeriod = webSocketConfig.pingPeriod
        timeout = webSocketConfig.timeout
    }
}