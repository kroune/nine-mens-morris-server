package com.example

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.test.Test

class ApplicationTest {
    @Test
    fun testRoot() {
        val client = HttpClient(CIO) {
            install(WebSockets)
        }
        runBlocking {
            val job1 = CoroutineScope(Dispatchers.IO).launch {
                client.webSocket("http://0.0.0.0:8080/api/v1/user/start-searching-game") {}
            }
            client.webSocket("http://0.0.0.0:8080/api/v1/user/start-searching-game") {
            }
            job1.join()
            client.webSocket("http://0.0.0.0:8080/api/v1/user/game-1") {
                this.send("{\"startIndex\":null,\"endIndex\":5}")
            }
        }
    }
}
