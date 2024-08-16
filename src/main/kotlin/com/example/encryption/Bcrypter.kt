package com.example.encryption

import at.favre.lib.crypto.bcrypt.BCrypt

object Bcrypter {
    const val COMPLEXITY = 6
    fun hash(value: String): String {
        return BCrypt.withDefaults().hashToString(COMPLEXITY, value.toCharArray())!!
    }

    fun verify(value: String?, hash: String?): Boolean {
        if (value == null || hash == null) {
            return false
        }
        return BCrypt.verifyer().verify(value.toCharArray(), hash).verified
    }
}