package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.CommandHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface CommandDao {
    @Query("SELECT * FROM command_history ORDER BY timestamp DESC")
    fun getAllCommands(): Flow<List<CommandHistory>>

    @Query("SELECT * FROM command_history ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentCommands(limit: Int = 10): Flow<List<CommandHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommand(command: CommandHistory): Long

    @Query("DELETE FROM command_history")
    suspend fun clearHistory()
}
