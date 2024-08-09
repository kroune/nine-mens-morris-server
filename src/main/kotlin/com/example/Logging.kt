package com.example

import io.ktor.server.websocket.*
import kotlinx.serialization.Serializable
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

val gameLogsPath = File(currentConfig.fileConfig.gameLogsPath)

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

val currentLogLevel = currentConfig.currentLogPriority

@Serializable
sealed class LogPriority(private val priority: Int) : Comparable<LogPriority> {
    @Serializable
    data object Errors : LogPriority(5)
    @Serializable
    data object Info : LogPriority(1)
    @Serializable
    data object Debug : LogPriority(0)

    override fun compareTo(other: LogPriority): Int {
        return this.priority - other.priority
    }
}
