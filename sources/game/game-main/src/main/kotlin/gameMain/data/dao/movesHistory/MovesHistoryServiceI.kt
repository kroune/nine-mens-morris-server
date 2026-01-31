package gameMain.data.dao.movesHistory

interface MovesHistoryServiceI {
    suspend fun fetchMoveHistory(gameId: Long): List<MovesHistoryStep>
}