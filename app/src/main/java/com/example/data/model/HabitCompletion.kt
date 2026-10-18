package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habit_completions")
data class HabitCompletion(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: Long,
    val dateString: String, // "yyyy-MM-dd"
    val completedCount: Int = 1,
    val timeSpentMinutes: Int = 0,
    val xpEarned: Int = 10,
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = ""
)
