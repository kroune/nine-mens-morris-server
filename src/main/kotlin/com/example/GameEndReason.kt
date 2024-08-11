package com.example

sealed class GameEndReason(val isFirstUser: Boolean) {
    class UserGaveUp(isFirstUserWon: Boolean) : GameEndReason(isFirstUserWon)
    class UserWasTooSlow(isFirstUserWon: Boolean) : GameEndReason(isFirstUserWon)
    class Normal(isFirstUserWon: Boolean) : GameEndReason(isFirstUserWon)
}