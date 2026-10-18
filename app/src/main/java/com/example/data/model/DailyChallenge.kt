package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_challenges")
data class DailyChallenge(
    @PrimaryKey val dateString: String, // "yyyy-MM-dd"
    val title: String,
    val description: String,
    val targetHabitCount: Int = 3,
    val bonusXp: Int = 25,
    val currentProgress: Int = 0,
    val isCompleted: Boolean = false,
    val rewardClaimed: Boolean = false
)
