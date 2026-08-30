package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "command_history")
data class CommandHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val commandText: String,
    val intentType: String,
    val description: String,
    val timestamp: Long = System.currentTimeMillis(),
    val wasSuccessful: Boolean = true,
    val canUndo: Boolean = false,
    val undoActionType: String? = null,
    val undoActionValue: String? = null
)
