package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.GeminiApiClient
import com.example.data.model.Badge
import com.example.data.model.DailyChallenge
import com.example.data.model.Habit
import com.example.data.model.HabitCompletion
import com.example.data.model.UserProfile
import com.example.data.repository.HabitRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AiChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isFromUser: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    val repository = HabitRepository(application)

    private val _selectedTab = MutableStateFlow(0) // 0: Home, 1: Calendar, 2: Statistics, 3: AI Coach, 4: Profile
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _selectedDate = MutableStateFlow(getTodayDateString())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    val habits: StateFlow<List<Habit>> = repository.habits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userProfile: StateFlow<UserProfile?> = repository.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserProfile())

    val badges: StateFlow<List<Badge>> = repository.badges
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCompletions: StateFlow<List<HabitCompletion>> = repository.getAllCompletions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayCompletions: StateFlow<List<HabitCompletion>> = _selectedDate
        .flatMapLatest { date -> repository.getCompletionsForDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dailyChallenge: StateFlow<DailyChallenge?> = _selectedDate
        .flatMapLatest { date -> repository.getDailyChallenge(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // AI Coach Chat
    private val _aiMessages = MutableStateFlow<List<AiChatMessage>>(
        listOf(
            AiChatMessage(
                text = "👋 Hello Hero! I'm your AI Coach. I analyze your consistency, suggest habit tweaks, and keep your streaks burning. How can I guide you today?"
            )
        )
    )
    val aiMessages: StateFlow<List<AiChatMessage>> = _aiMessages.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    private val _exportJsonResult = MutableStateFlow<String?>(null)
    val exportJsonResult: StateFlow<String?> = _exportJsonResult.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    init {
        viewModelScope.launch {
            repository.recalculateStreak()
            repository.checkBadgeUnlocks()
            repository.updateDailyChallenge(getTodayDateString(), delta = 0)
        }
    }

    fun setSelectedTab(index: Int) {
        _selectedTab.value = index
    }

    fun setSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setSelectedDate(dateString: String) {
        _selectedDate.value = dateString
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun toggleHabitCompletion(habitId: Long, dateString: String = _selectedDate.value, timeSpentMinutes: Int = 15) {
        viewModelScope.launch {
            val isCompleted = repository.toggleHabitCompletion(habitId, dateString, timeSpentMinutes)
            val msg = if (isCompleted) "⚡ Habit Completed! +10 XP" else "Habit unchecked"
            _toastMessage.value = msg
        }
    }

    fun addHabit(
        title: String,
        description: String,
        category: String,
        colorHex: String,
        targetCount: Int,
        unit: String,
        reminderTime: String?
    ) {
        viewModelScope.launch {
            val habit = Habit(
                title = title,
                description = description,
                category = category,
                colorHex = colorHex,
                targetCount = targetCount,
                unit = unit,
                reminderTime = reminderTime
            )
            repository.insertHabit(habit)
            _toastMessage.value = "Habit '$title' created!"
        }
    }

    fun editHabit(habit: Habit) {
        viewModelScope.launch {
            repository.updateHabit(habit)
            _toastMessage.value = "Habit updated"
        }
    }

    fun deleteHabit(habitId: Long) {
        viewModelScope.launch {
            repository.deleteHabit(habitId)
            _toastMessage.value = "Habit deleted"
        }
    }

    fun updateProfileNameAndEmail(name: String, email: String) {
        viewModelScope.launch {
            val current = userProfile.value ?: UserProfile()
            repository.updateUserProfile(current.copy(name = name, email = email, isLoggedIn = true))
            _toastMessage.value = "Profile saved"
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            val current = userProfile.value ?: UserProfile()
            repository.updateUserProfile(current.copy(themeMode = mode))
        }
    }

    fun toggleNotifications(enabled: Boolean) {
        viewModelScope.launch {
            val current = userProfile.value ?: UserProfile()
            repository.updateUserProfile(current.copy(notificationsEnabled = enabled))
        }
    }

    // AI Functions
    fun askAiCoach(userQuery: String) {
        if (userQuery.isBlank()) return
        val userMsg = AiChatMessage(text = userQuery, isFromUser = true)
        _aiMessages.value = _aiMessages.value + userMsg
        _isAiLoading.value = true

        viewModelScope.launch {
            val profile = userProfile.value
            val habitList = habits.value
            val completions = allCompletions.value

            val systemContext = """
                You are HabitHero AI Coach, an inspiring, empathetic, and highly actionable habit tracking assistant.
                User Context:
                - Name: ${profile?.name ?: "Hero"}
                - Level: ${profile?.level ?: 1}, Total XP: ${profile?.totalXp ?: 0}
                - Current Streak: ${profile?.currentStreak ?: 0} days, Longest Streak: ${profile?.longestStreak ?: 0} days
                - Active Habits: ${habitList.joinToString { it.title }}
                - Total Habit Logs: ${completions.size}
            """.trimIndent()

            val response = GeminiApiClient.generateContent(
                prompt = userQuery,
                systemInstruction = systemContext
            )

            _isAiLoading.value = false
            _aiMessages.value = _aiMessages.value + AiChatMessage(text = response, isFromUser = false)
        }
    }

    fun requestAiProgressAnalysis() {
        askAiCoach("Please analyze my recent habit progress and give me 3 actionable tips.")
    }

    fun requestAiStreakMotivation() {
        askAiCoach("Generate a powerful motivational message based on my current streak.")
    }

    fun requestAiHabitRecommendations(goal: String) {
        askAiCoach("Recommend 4 new habits tailored for my goal: $goal.")
    }

    fun requestAiGoalAdjustment() {
        askAiCoach("Suggest easier goal adjustments for habits that are hard to keep up with.")
    }

    fun exportData() {
        viewModelScope.launch {
            val json = repository.exportDataToJson()
            _exportJsonResult.value = json
            _toastMessage.value = "Data exported to JSON!"
        }
    }

    fun importData(jsonString: String) {
        viewModelScope.launch {
            val success = repository.importDataFromJson(jsonString)
            _toastMessage.value = if (success) "Backup restored successfully!" else "Failed to parse JSON backup"
        }
    }

    private fun getTodayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }
}
