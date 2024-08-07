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
        println("$currentDate $text")
        game.appendText("$currentDate $text")
    }
}

fun log(text: String, logLevel: LogPriority = LogPriority.Info) {
    val shouldPrint = logLevel >= currentLogLevel
    if (!shouldPrint) {
        return
    }
    val sdf = SimpleDateFormat("hh:mm:ss dd/M/yyyy ")
    val currentDate = sdf.format(Date())
    println("$currentDate $text")
}

val currentLogLevel = when (System.getenv("CURRENT_LOG_LEVEL")) {
    "DEBUG" -> LogPriority.Debug
    "INFO" -> LogPriority.Info
    else -> LogPriority.Debug
}

sealed class LogPriority(private val priority: Int) : Comparable<LogPriority> {
    data object Errors : LogPriority(5)
    data object Info : LogPriority(1)
    data object Debug : LogPriority(0)

    override fun compareTo(other: LogPriority): Int {
        return this.priority - other.priority
    }
}
