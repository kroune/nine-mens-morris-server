package com.example.data.users

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class UserData(
    val login: String,
    val password: String,
    val date: LocalDate = Clock.System.now().toLocalDateTime(TimeZone.UTC).date,
    var rating: Int = 1000,
    var profilePicture: ByteArray? = null
)