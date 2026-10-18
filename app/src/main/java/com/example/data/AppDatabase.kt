package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.BadgeDao
import com.example.data.dao.DailyChallengeDao
import com.example.data.dao.HabitCompletionDao
import com.example.data.dao.HabitDao
import com.example.data.dao.UserProfileDao
import com.example.data.model.Badge
import com.example.data.model.DailyChallenge
import com.example.data.model.Habit
import com.example.data.model.HabitCompletion
import com.example.data.model.UserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Habit::class,
        HabitCompletion::class,
        UserProfile::class,
        Badge::class,
        DailyChallenge::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun habitCompletionDao(): HabitCompletionDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun badgeDao(): BadgeDao
    abstract fun dailyChallengeDao(): DailyChallengeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "habit_hero_database"
                )
                .addCallback(DatabaseCallback(context))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(
        private val context: Context
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    populateInitialData(database)
                }
            }
        }

        private suspend fun populateInitialData(database: AppDatabase) {
            // Initial Badges
            val initialBadges = listOf(
                Badge("FIRST_STEP", "First Step", "Complete your very first habit", "SPECIAL", "CheckCircle", requiredCompletions = 1),
                Badge("STREAK_7", "7-Day Warrior", "Maintain a 7-day completion streak", "STREAK", "Fire", requiredStreak = 7),
                Badge("STREAK_30", "30-Day Master", "Maintain a 30-day completion streak", "STREAK", "Crown", requiredStreak = 30),
                Badge("STREAK_100", "100-Day Legend", "Maintain a 100-day completion streak", "STREAK", "Star", requiredStreak = 100),
                Badge("STREAK_365", "365-Day Hero", "Maintain a full year 365-day streak!", "STREAK", "Trophy", requiredStreak = 365),
                Badge("XP_100", "Level 2 Novice", "Earn 100 XP in total", "XP", "Bolt", requiredXp = 100),
                Badge("XP_500", "500 XP Champion", "Earn 500 XP in total", "XP", "Medal", requiredXp = 500),
                Badge("HABIT_CREATOR", "Habit Builder", "Create 5 active habits", "HABIT", "Add", requiredCompletions = 5)
            )
            database.badgeDao().insertBadges(initialBadges)

            // Initial User Profile
            database.userProfileDao().insertOrUpdateProfile(
                UserProfile(
                    id = 1,
                    name = "Habit Explorer",
                    email = "hero@habithero.app",
                    isLoggedIn = true,
                    totalXp = 0,
                    level = 1,
                    currentStreak = 0,
                    longestStreak = 0
                )
            )

            // Initial Starter Habits
            val defaultHabits = listOf(
                Habit(title = "Morning Hydration", description = "Drink 2 glasses of water after waking up", category = "Health", colorHex = "#10B981", iconName = "WaterDrop", targetCount = 2, unit = "glasses", reminderTime = "08:00"),
                Habit(title = "Daily Mindful Walk", description = "Take a 15 min refreshing outdoor walk", category = "Fitness", colorHex = "#3B82F6", iconName = "DirectionsWalk", targetCount = 15, unit = "mins", reminderTime = "17:30"),
                Habit(title = "Read 10 Pages", description = "Read non-fiction or motivational book", category = "Learning", colorHex = "#8B5CF6", iconName = "MenuBook", targetCount = 10, unit = "pages", reminderTime = "21:00")
            )
            defaultHabits.forEach { database.habitDao().insertHabit(it) }
        }
    }
}
