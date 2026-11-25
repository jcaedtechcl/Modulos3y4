package com.example.modulos3y4.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "users", indices = [Index(value = ["username"], unique = true)])
data class User(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    // La contraseña nunca se guarda en texto plano: se almacena su hash SHA-256 con una sal aleatoria.
    val passwordHash: String,
    val salt: String
)
