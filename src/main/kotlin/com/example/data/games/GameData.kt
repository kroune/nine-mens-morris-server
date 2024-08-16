package com.example.data.games

import com.kroune.nineMensMorrisLib.Position
import com.kroune.nineMensMorrisLib.gameStartPosition
import com.kroune.nineMensMorrisLib.move.Movement
import kotlin.random.Random

class GameData(
    val firstPlayerId: Long,
    val secondPlayerId: Long,
    val botId: Long?,
    val movesHistory: List<Movement> = listOf(),
    val position: Position = gameStartPosition,
    val firstPlayerMovesFirst: Boolean = Random.nextBoolean(),
    val movesCount: Int = 0
)
