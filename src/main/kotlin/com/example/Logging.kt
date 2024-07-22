package com.example

import io.ktor.server.websocket.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

val gameLogsPath = File("logs/game")

fun DefaultWebSocketServerSession.log(gameId: Long, text: String) {
    gameLogsPath.mkdirs()
    run {
        val game = File(gameLogsPath, gameId.toString())
        if (!game.exists()) {
            game.createNewFile()
        }
        val sdf = SimpleDateFormat("hh:mm:ss dd/M/yyyy ")
        val currentDate = sdf.format(Date())
        game.appendText("$currentDate $text")
    }
}