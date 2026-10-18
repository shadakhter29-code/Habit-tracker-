package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val category: String = "Health", // Health, Productivity, Fitness, Mindfulness, Finance, Learning, Custom
    val colorHex: String = "#10B981",
    val iconName: String = "CheckCircle",
    val frequencyType: String = "DAILY", // DAILY, WEEKLY, SPECIFIC_DAYS
    val selectedDays: String = "1,2,3,4,5,6,7", // 1=Mon, 7=Sun
    val targetCount: Int = 1,
    val unit: String = "times", // times, mins, glasses, pages, km
    val reminderTime: String? = "09:00", // "HH:mm" or null
    val defaultDurationMins: Int = 15,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
