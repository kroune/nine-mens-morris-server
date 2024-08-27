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
package com.example

import kotlinx.serialization.Serializable
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

val gameLogsPath = File(currentConfig.fileConfig.gameLogsPath)

fun log(gameId: Long, text: String) {
    gameLogsPath.mkdirs()
    run {
        val game = File(gameLogsPath, gameId.toString())
        if (!game.exists()) {
            game.createNewFile()
        }
        val sdf = SimpleDateFormat("hh:mm:ss dd/M/yyyy ")
        val currentDate = sdf.format(Date())
        println("$currentDate $text")
        game.appendText("$currentDate $text\n")
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
