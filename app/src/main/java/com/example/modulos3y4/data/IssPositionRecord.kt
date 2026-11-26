package com.example.modulos3y4.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Posicion que el usuario decidio conservar.
 *
 * Es una tabla aparte de [IssPosition] a proposito: aquella guarda una sola fila
 * que cada descarga sobrescribe (la lectura actual), mientras que aqui se
 * acumula solo lo que el usuario elige guardar. Descargar no implica almacenar.
 */
@Entity(tableName = "iss_position_history")
data class IssPositionRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val latitude: Double,
    val longitude: Double,
    /** Marca de tiempo que informa la API, en segundos desde epoch. */
    val timestamp: Long,
    /** Momento en que el usuario guardo el registro, en milisegundos desde epoch. */
    val savedAt: Long
)
