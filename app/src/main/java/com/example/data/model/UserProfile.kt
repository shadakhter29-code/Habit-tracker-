package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val name: String = "Habit Explorer",
    val email: String = "hero@habithero.app",
    val isLoggedIn: Boolean = false,
    val totalXp: Int = 0,
    val level: Int = 1,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val themeMode: String = "SYSTEM", // LIGHT, DARK, SYSTEM
    val notificationsEnabled: Boolean = true,
    val defaultReminderTime: String = "09:00",
    val lastActiveDate: String = "",
    val cloudSyncEnabled: Boolean = true,
    val lastSyncTimestamp: Long = System.currentTimeMillis()
)
