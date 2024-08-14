package com.example.data.users

import kotlinx.datetime.LocalDate

data class UserData(
    val login: String,
    val password: String,
    val date: LocalDate,
    var rating: Int = 1000,
    var profilePicture: ByteArray? = null
)