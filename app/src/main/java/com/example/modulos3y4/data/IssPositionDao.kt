package com.example.modulos3y4.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface IssPositionDao {

    @Upsert
    suspend fun upsert(position: IssPosition)

    /**
     * Room devuelve un Flow que vuelve a emitir cada vez que cambia la tabla, de
     * modo que la UI se actualiza sola cuando el Worker guarda una posicion nueva.
     * No es `suspend`: Room se encarga de observar y consultar fuera del hilo principal.
     */
    @Query("SELECT * FROM iss_position WHERE id = :id LIMIT 1")
    fun observeLatest(id: Int = IssPosition.SINGLE_ROW_ID): Flow<IssPosition?>
}
