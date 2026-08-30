package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.NexusReminder
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM nexus_reminders ORDER BY targetTimeMillis ASC")
    fun getAllReminders(): Flow<List<NexusReminder>>

    @Query("SELECT * FROM nexus_reminders WHERE isCompleted = 0 ORDER BY targetTimeMillis ASC")
    fun getPendingReminders(): Flow<List<NexusReminder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: NexusReminder): Long

    @Update
    suspend fun updateReminder(reminder: NexusReminder)

    @Query("DELETE FROM nexus_reminders WHERE id = :id")
    suspend fun deleteReminderById(id: Long)

    @Query("DELETE FROM nexus_reminders")
    suspend fun clearAllReminders()
}
