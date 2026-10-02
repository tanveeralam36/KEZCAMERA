package com.example.data.db

import androidx.room.*
import com.example.data.model.Script
import kotlinx.coroutines.flow.Flow

@Dao
interface ScriptDao {
    @Query("SELECT * FROM scripts ORDER BY updatedAt DESC")
    fun getAllScripts(): Flow<List<Script>>

    @Query("SELECT * FROM scripts WHERE id = :id")
    suspend fun getScriptById(id: Long): Script?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScript(script: Script): Long

    @Update
    suspend fun updateScript(script: Script)

    @Delete
    suspend fun deleteScript(script: Script)

    @Query("SELECT COUNT(*) FROM scripts")
    suspend fun getScriptCount(): Int
}
