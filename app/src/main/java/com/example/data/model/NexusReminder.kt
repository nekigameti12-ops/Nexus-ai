package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "nexus_reminders")
data class NexusReminder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val targetTimeMillis: Long,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
