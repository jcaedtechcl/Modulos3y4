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

    @Query("DELETE FROM iss_position_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM iss_position_history")
    suspend fun clear()
}
