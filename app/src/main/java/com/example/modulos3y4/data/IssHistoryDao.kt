package com.example.modulos3y4.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface IssHistoryDao {

    @Insert
    suspend fun insert(record: IssPositionRecord): Long

    /** Mas recientes primero: es el orden en que la pantalla los muestra. */
    @Query("SELECT * FROM iss_position_history ORDER BY savedAt DESC")
    fun observeAll(): Flow<List<IssPositionRecord>>

    /**
     * Registros que faltan por subir, del mas antiguo al mas nuevo para que se
     * suban en el orden en que el usuario los guardo.
     */
    @Query("SELECT * FROM iss_position_history WHERE isSynced = 0 ORDER BY savedAt ASC")
    suspend fun pendingSync(): List<IssPositionRecord>

    @Query("UPDATE iss_position_history SET isSynced = 1 WHERE id = :id")
    suspend fun markSynced(id: Long)

    @Query("DELETE FROM iss_position_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM iss_position_history")
    suspend fun clear()
}
