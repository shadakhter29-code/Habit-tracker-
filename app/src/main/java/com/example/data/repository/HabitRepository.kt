package com.example.data.repository

import android.content.Context
import com.example.data.AppDatabase
import com.example.data.model.Badge
import com.example.data.model.DailyChallenge
import com.example.data.model.Habit
import com.example.data.model.HabitCompletion
import com.example.data.model.UserProfile
import com.example.notification.HabitNotificationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HabitRepository(private val context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val habitDao = db.habitDao()
    private val completionDao = db.habitCompletionDao()
    private val profileDao = db.userProfileDao()
    private val badgeDao = db.badgeDao()
    private val challengeDao = db.dailyChallengeDao()

    val habits: Flow<List<Habit>> = habitDao.getAllHabits()
    val userProfile: Flow<UserProfile?> = profileDao.getUserProfile()
    val badges: Flow<List<Badge>> = badgeDao.getAllBadges()

    fun getCompletionsForDate(dateString: String): Flow<List<HabitCompletion>> =
        completionDao.getCompletionsForDate(dateString)

    fun getCompletionsForHabit(habitId: Long): Flow<List<HabitCompletion>> =
        completionDao.getCompletionsForHabit(habitId)

    fun getDailyChallenge(dateString: String): Flow<DailyChallenge?> =
        challengeDao.getChallengeForDate(dateString)

    fun getAllCompletions(): Flow<List<HabitCompletion>> =
        completionDao.getAllCompletions()

    suspend fun getTodayDateString(): String = withContext(Dispatchers.IO) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        sdf.format(Date())
    }

    suspend fun insertHabit(habit: Habit): Long = withContext(Dispatchers.IO) {
        val id = habitDao.insertHabit(habit)
        if (!habit.reminderTime.isNullOrBlank()) {
            HabitNotificationManager.scheduleHabitReminder(context, id, habit.title, habit.reminderTime)
        }
        checkBadgeUnlocks()
        id
    }

    suspend fun updateHabit(habit: Habit) = withContext(Dispatchers.IO) {
        habitDao.updateHabit(habit)
        if (!habit.reminderTime.isNullOrBlank()) {
            HabitNotificationManager.scheduleHabitReminder(context, habit.id, habit.title, habit.reminderTime)
        } else {
            HabitNotificationManager.cancelReminder(context, habit.id)
        }
    }

    suspend fun deleteHabit(habitId: Long) = withContext(Dispatchers.IO) {
        habitDao.deleteHabitById(habitId)
        HabitNotificationManager.cancelReminder(context, habitId)
    }

    suspend fun toggleHabitCompletion(habitId: Long, dateString: String, timeSpentMinutes: Int = 15): Boolean = withContext(Dispatchers.IO) {
        val existing = completionDao.getCompletionForHabitAndDate(habitId, dateString)
        val habit = habitDao.getHabitByIdDirect(habitId) ?: return@withContext false

        if (existing != null) {
            // Undo completion
            completionDao.deleteCompletionForDate(habitId, dateString)
            decreaseXp(10)
            recalculateStreak()
            updateDailyChallenge(dateString, delta = -1)
            false
        } else {
            // Complete habit
            val completion = HabitCompletion(
                habitId = habitId,
                dateString = dateString,
                completedCount = habit.targetCount,
                timeSpentMinutes = if (timeSpentMinutes > 0) timeSpentMinutes else habit.defaultDurationMins,
                xpEarned = 10
            )
            completionDao.insertCompletion(completion)
            addXp(10)
            recalculateStreak()
            updateDailyChallenge(dateString, delta = 1)
            checkBadgeUnlocks()
            true
        }
    }

    suspend fun updateDailyChallenge(dateString: String, delta: Int) = withContext(Dispatchers.IO) {
        var challenge = challengeDao.getChallengeForDateDirect(dateString)
        if (challenge == null) {
            challenge = DailyChallenge(
                dateString = dateString,
                title = "Daily Power Trio",
                description = "Complete 3 habits today for a bonus 25 XP!",
                targetHabitCount = 3,
                bonusXp = 25,
                currentProgress = 0
            )
        }

        val newProgress = (challenge.currentProgress + delta).coerceAtLeast(0)
        val isCompleted = newProgress >= challenge.targetHabitCount
        var rewardClaimed = challenge.rewardClaimed

        if (isCompleted && !rewardClaimed) {
            addXp(challenge.bonusXp)
            rewardClaimed = true
        }

        val updated = challenge.copy(
            currentProgress = newProgress,
            isCompleted = isCompleted,
            rewardClaimed = rewardClaimed
        )
        challengeDao.insertOrUpdateChallenge(updated)
    }

    private suspend fun addXp(amount: Int) {
        val current = profileDao.getUserProfileDirect() ?: UserProfile()
        val newXp = current.totalXp + amount
        val newLevel = (newXp / 100) + 1
        profileDao.insertOrUpdateProfile(
            current.copy(
                totalXp = newXp,
                level = newLevel,
                lastActiveDate = getTodayDateString()
            )
        )
    }

    private suspend fun decreaseXp(amount: Int) {
        val current = profileDao.getUserProfileDirect() ?: UserProfile()
        val newXp = (current.totalXp - amount).coerceAtLeast(0)
        val newLevel = (newXp / 100) + 1
        profileDao.insertOrUpdateProfile(current.copy(totalXp = newXp, level = newLevel))
    }

    suspend fun recalculateStreak() = withContext(Dispatchers.IO) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        val todayStr = sdf.format(cal.time)

        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayStr = sdf.format(cal.time)

        val completions = completionDao.getAllCompletions().firstOrNull() ?: emptyList()
        val completedDates = completions.map { it.dateString }.toSet()

        var streak = 0
        val checkCal = Calendar.getInstance()

        // Check if completed today or yesterday to start streak
        val hasCompletedToday = completedDates.contains(todayStr)
        val hasCompletedYesterday = completedDates.contains(yesterdayStr)

        if (hasCompletedToday) {
            streak++
            checkCal.add(Calendar.DAY_OF_YEAR, -1)
            while (completedDates.contains(sdf.format(checkCal.time))) {
                streak++
                checkCal.add(Calendar.DAY_OF_YEAR, -1)
            }
        } else if (hasCompletedYesterday) {
            while (completedDates.contains(sdf.format(checkCal.time))) {
                streak++
                checkCal.add(Calendar.DAY_OF_YEAR, -1)
            }
        }

        val profile = profileDao.getUserProfileDirect() ?: UserProfile()
        val newLongest = maxOf(profile.longestStreak, streak)
        profileDao.insertOrUpdateProfile(
            profile.copy(
                currentStreak = streak,
                longestStreak = newLongest,
                lastActiveDate = todayStr
            )
        )
    }

    suspend fun checkBadgeUnlocks() = withContext(Dispatchers.IO) {
        val profile = profileDao.getUserProfileDirect() ?: return@withContext
        val totalCompletions = completionDao.getTotalCompletionsCount()
        val badgesList = badgeDao.getAllBadges().firstOrNull() ?: emptyList()

        badgesList.forEach { badge ->
            if (!badge.isUnlocked) {
                var shouldUnlock = false
                if (badge.requiredStreak > 0 && profile.currentStreak >= badge.requiredStreak) shouldUnlock = true
                if (badge.requiredXp > 0 && profile.totalXp >= badge.requiredXp) shouldUnlock = true
                if (badge.requiredCompletions > 0 && totalCompletions >= badge.requiredCompletions) shouldUnlock = true

                if (shouldUnlock) {
                    badgeDao.updateBadge(badge.copy(isUnlocked = true, unlockedTimestamp = System.currentTimeMillis()))
                }
            }
        }
    }

    suspend fun updateUserProfile(profile: UserProfile) = withContext(Dispatchers.IO) {
        profileDao.insertOrUpdateProfile(profile)
    }

    suspend fun exportDataToJson(): String = withContext(Dispatchers.IO) {
        val habitsList = habitDao.getAllHabits().firstOrNull() ?: emptyList()
        val completionsList = completionDao.getAllCompletions().firstOrNull() ?: emptyList()
        val profile = profileDao.getUserProfileDirect() ?: UserProfile()

        val jsonRoot = JSONObject().apply {
            put("app", "HabitHero")
            put("version", 1)
            put("exportedAt", System.currentTimeMillis())

            val profileObj = JSONObject().apply {
                put("name", profile.name)
                put("email", profile.email)
                put("totalXp", profile.totalXp)
                put("level", profile.level)
                put("currentStreak", profile.currentStreak)
                put("longestStreak", profile.longestStreak)
                put("themeMode", profile.themeMode)
            }
            put("profile", profileObj)

            val habitsArray = JSONArray()
            habitsList.forEach { h ->
                habitsArray.put(JSONObject().apply {
                    put("id", h.id)
                    put("title", h.title)
                    put("description", h.description)
                    put("category", h.category)
                    put("colorHex", h.colorHex)
                    put("targetCount", h.targetCount)
                    put("unit", h.unit)
                    put("reminderTime", h.reminderTime ?: "")
                })
            }
            put("habits", habitsArray)

            val completionsArray = JSONArray()
            completionsList.forEach { c ->
                completionsArray.put(JSONObject().apply {
                    put("habitId", c.habitId)
                    put("dateString", c.dateString)
                    put("timeSpentMinutes", c.timeSpentMinutes)
                    put("xpEarned", c.xpEarned)
                })
            }
            put("completions", completionsArray)
        }
        jsonRoot.toString(2)
    }

    suspend fun importDataFromJson(jsonString: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            if (root.optString("app") != "HabitHero") return@withContext false

            val profileObj = root.optJSONObject("profile")
            if (profileObj != null) {
                val current = profileDao.getUserProfileDirect() ?: UserProfile()
                profileDao.insertOrUpdateProfile(
                    current.copy(
                        name = profileObj.optString("name", current.name),
                        email = profileObj.optString("email", current.email),
                        totalXp = profileObj.optInt("totalXp", current.totalXp),
                        level = profileObj.optInt("level", current.level),
                        currentStreak = profileObj.optInt("currentStreak", current.currentStreak),
                        longestStreak = profileObj.optInt("longestStreak", current.longestStreak),
                        themeMode = profileObj.optString("themeMode", current.themeMode)
                    )
                )
            }

            val habitsArray = root.optJSONArray("habits")
            if (habitsArray != null) {
                for (i in 0 until habitsArray.length()) {
                    val obj = habitsArray.getJSONObject(i)
                    val habit = Habit(
                        title = obj.getString("title"),
                        description = obj.optString("description", ""),
                        category = obj.optString("category", "Health"),
                        colorHex = obj.optString("colorHex", "#10B981"),
                        targetCount = obj.optInt("targetCount", 1),
                        unit = obj.optString("unit", "times"),
                        reminderTime = if (obj.optString("reminderTime").isBlank()) null else obj.optString("reminderTime")
                    )
                    habitDao.insertHabit(habit)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
