package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "badges")
data class Badge(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val category: String, // STREAK, XP, HABIT, SPECIAL
    val iconName: String,
    val requiredStreak: Int = 0,
    val requiredXp: Int = 0,
    val requiredCompletions: Int = 0,
    val isUnlocked: Boolean = false,
    val unlockedTimestamp: Long = 0L
)
