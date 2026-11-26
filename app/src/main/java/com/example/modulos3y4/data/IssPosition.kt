package com.example.modulos3y4.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Ultima posicion descargada de la ISS.
 *
 * Solo interesa el dato mas reciente, asi que la tabla guarda una unica fila con
 * un id fijo: cada descarga la reemplaza mediante @Upsert. Persistirla en Room
 * permite que la pantalla muestre el ultimo valor conocido incluso si la app se
 * cierra o el proceso muere mientras el Worker esta corriendo.
 */
@Entity(tableName = "iss_position")
data class IssPosition(
    @PrimaryKey val id: Int = SINGLE_ROW_ID,
    val latitude: Double,
    val longitude: Double,
    /** Marca de tiempo que informa la API, en segundos desde epoch. */
    val timestamp: Long,
    /** Momento local de la descarga, en milisegundos desde epoch. */
    val fetchedAt: Long
) {
    companion object {
        const val SINGLE_ROW_ID = 0
    }
}
