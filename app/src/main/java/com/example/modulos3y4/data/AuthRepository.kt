package com.example.modulos3y4.data

import android.database.sqlite.SQLiteConstraintException
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

sealed interface AuthResult {
    data class Success(val username: String) : AuthResult
    data class Error(val message: String) : AuthResult
}

class AuthRepository(private val userDao: UserDao) {

    suspend fun register(username: String, password: String): AuthResult {
        val user = username.trim()
        if (user.isEmpty() || password.isEmpty()) {
            return AuthResult.Error("Ingresa usuario y contraseña")
        }
        val salt = newSalt()
        return try {
            userDao.insert(User(username = user, passwordHash = hash(password, salt), salt = salt))
            AuthResult.Success(user)
        } catch (_: SQLiteConstraintException) {
            AuthResult.Error("El usuario ya existe")
        }
    }

    suspend fun login(username: String, password: String): AuthResult {
        val user = username.trim()
        if (user.isEmpty() || password.isEmpty()) {
            return AuthResult.Error("Ingresa usuario y contraseña")
        }
        val stored = userDao.findByUsername(user)
        return if (stored != null && MessageDigest.isEqual(
                hash(password, stored.salt).toByteArray(),
                stored.passwordHash.toByteArray()
            )
        ) {
            AuthResult.Success(stored.username)
        } else {
            AuthResult.Error("Usuario o contraseña incorrectos")
        }
    }

    private fun newSalt(): String {
        val bytes = ByteArray(16).also { SecureRandom().nextBytes(it) }
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun hash(password: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt.toByteArray())
        return Base64.encodeToString(digest.digest(password.toByteArray()), Base64.NO_WRAP)
    }
}
