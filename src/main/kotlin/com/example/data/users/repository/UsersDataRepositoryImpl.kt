package com.example.data.users.repository

import com.example.data.users.UserData
import com.example.data.users.UsersDataTable
import com.example.encryption.Bcrypter
import com.example.encryption.JwtTokenImpl
import kotlinx.datetime.LocalDate
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

class UsersDataRepositoryImpl : UsersDataRepositoryI {
    init {
        transaction {
            SchemaUtils.create(UsersDataTable)
        }
    }

    override suspend fun create(data: UserData) {
        newSuspendedTransaction {
            val passwordHashValue = Bcrypter.hash(data.password)
            UsersDataTable.insert {
                it[login] = data.login
                it[passwordHash] = passwordHashValue
                it[rating] = data.rating
                it[creationDate] = data.date
                it[profilePicture] = data.profilePicture
            }
        }
    }

    override suspend fun getIdByLogin(login: String): Long? {
        return newSuspendedTransaction {
            UsersDataTable.selectAll()
                .where {
                    UsersDataTable.login eq login
                }.map {
                    it[UsersDataTable.id]
                }.firstOrNull()
        }
    }

    override suspend fun getLoginById(id: Long): String? {
        return newSuspendedTransaction {
            UsersDataTable.selectAll()
                .where {
                    UsersDataTable.id eq id
                }.limit(1).map {
                    it[UsersDataTable.login]
                }.firstOrNull()
        }
    }

    override suspend fun updatePictureByLogin(login: String, newPicture: ByteArray) {
        newSuspendedTransaction {
            UsersDataTable.update(
                { UsersDataTable.login eq login }
            ) {
                it[profilePicture] = newPicture
            }
        }
    }

    override suspend fun updatePictureById(id: Long, newPicture: ByteArray) {
        newSuspendedTransaction {
            UsersDataTable.update(
                { UsersDataTable.id eq id }
            ) {
                it[profilePicture] = newPicture
            }
        }
    }

    override suspend fun getPictureById(id: Long): ByteArray? {
        return newSuspendedTransaction {
            UsersDataTable.selectAll()
                .where {
                    UsersDataTable.id eq id
                }.limit(1).map {
                    it[UsersDataTable.profilePicture]
                }.firstOrNull()
        }
    }

    override suspend fun getRatingByLogin(login: String): Int? {
        return newSuspendedTransaction {
            UsersDataTable.selectAll()
                .where {
                    UsersDataTable.login eq login
                }.limit(1).map {
                    it[UsersDataTable.rating]
                }.firstOrNull()
        }
    }

    override suspend fun getRatingById(id: Long): Int? {
        return newSuspendedTransaction {
            UsersDataTable.selectAll()
                .where {
                    UsersDataTable.id eq id
                }.limit(1).map {
                    it[UsersDataTable.rating]
                }.firstOrNull()
        }
    }

    override suspend fun getCreationDateById(id: Long): LocalDate? {
        return newSuspendedTransaction {
            UsersDataTable.selectAll()
                .where {
                    UsersDataTable.id eq id
                }.limit(1).map {
                    it[UsersDataTable.creationDate]
                }.firstOrNull()
        }
    }

    override suspend fun updateRatingByLogin(login: String, newRating: Int) {
        newSuspendedTransaction {
            UsersDataTable.update(
                { UsersDataTable.login eq login }
            ) {
                it[rating] = newRating
            }
        }
    }

    override suspend fun updateRatingById(id: Long, newRating: Int) {
        newSuspendedTransaction {
            UsersDataTable.update(
                { UsersDataTable.id eq id }
            ) {
                it[rating] = newRating
            }
        }
    }

    override suspend fun getIdByJwtToken(jwtToken: String): Long? {
        val login = JwtTokenImpl(jwtToken).getLogin().getOrNull() ?: return null
        return getIdByLogin(login)
    }

    override suspend fun isLoginPresent(login: String): Boolean {
        return newSuspendedTransaction {
            UsersDataTable.selectAll()
                .where {
                    UsersDataTable.login eq login
                }.limit(1).map {
                    it[UsersDataTable.login]
                }.any()
        }
    }

    override suspend fun exists(login: String, password: String): Boolean {
        return newSuspendedTransaction {
            Bcrypter.verify(
                password,
                UsersDataTable.selectAll()
                    .where {
                        UsersDataTable.login eq login
                    }.limit(1).map {
                        it[UsersDataTable.passwordHash]
                    }.firstOrNull()
            )
        }
    }
}