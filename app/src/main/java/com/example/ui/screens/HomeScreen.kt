package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Habit
import com.example.ui.components.AddEditHabitBottomSheet
import com.example.ui.components.DailyChallengeCard
import com.example.ui.components.HabitCard
import com.example.ui.components.XpLevelBar
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val habits by viewModel.habits.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val todayCompletions by viewModel.todayCompletions.collectAsState()
    val dailyChallenge by viewModel.dailyChallenge.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()

    var showAddBottomSheet by remember { mutableStateOf(false) }
    var habitToEdit by remember { mutableStateOf<Habit?>(null) }

    val categories = listOf("All", "Health", "Productivity", "Fitness", "Mindfulness", "Finance", "Learning")

    val completedHabitIds = remember(todayCompletions) {
        todayCompletions.map { it.habitId }.toSet()
    }

    val filteredHabits = remember(habits, selectedCategory) {
        if (selectedCategory == "All") habits
        else habits.filter { it.category.equals(selectedCategory, ignoreCase = true) }
    }

    val sheetState = rememberModalBottomSheetState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    habitToEdit = null
                    showAddBottomSheet = true
                },
                containerColor = EmeraldPrimary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.testTag("add_habit_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Habit")
            }
        },
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Header
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Welcome, ${userProfile?.name ?: "Hero"} 👋",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Build tiny habits, forge huge streaks",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            color = EmeraldPrimary.copy(alpha = 0.15f),
                            shape = CircleShape
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ElectricBolt,
                                    contentDescription = "Gamified",
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // XP Level Bar
                    XpLevelBar(profile = userProfile)

                    Spacer(modifier = Modifier.height(12.dp))

                    // Daily Challenge
                    DailyChallengeCard(challenge = dailyChallenge)
                }
            }

            // Category Filter Pills
            item {
                Column {
                    Text(
                        text = "Today's Habits",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(categories) { category ->
                            FilterChip(
                                selected = selectedCategory == category,
                                onClick = { viewModel.setSelectedCategory(category) },
                                label = { Text(category, fontSize = 12.sp) }
                            )
                        }
                    }
                }
            }

            // Habit List
            if (filteredHabits.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = EmeraldPrimary.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (habits.isEmpty()) "No habits created yet!" else "No habits in this category",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap the + button to add your first daily habit",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                habitToEdit = null
                                showAddBottomSheet = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Create Habit (+10 XP)")
                        }
                    }
                }
            } else {
                items(
                    items = filteredHabits,
                    key = { it.id }
                ) { habit ->
                    val isDone = completedHabitIds.contains(habit.id)
                    HabitCard(
                        habit = habit,
                        isCompleted = isDone,
                        onToggleCompletion = {
                            viewModel.toggleHabitCompletion(habit.id, selectedDate)
                        },
                        onEdit = {
                            habitToEdit = habit
                            showAddBottomSheet = true
                        },
                        onDelete = {
                            viewModel.deleteHabit(habit.id)
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }

    if (showAddBottomSheet) {
        AddEditHabitBottomSheet(
            existingHabit = habitToEdit,
            sheetState = sheetState,
            onDismiss = { showAddBottomSheet = false },
            onSave = { title, desc, cat, color, target, unit, reminder ->
                if (habitToEdit == null) {
                    viewModel.addHabit(title, desc, cat, color, target, unit, reminder)
                } else {
                    val updated = habitToEdit!!.copy(
                        title = title,
                        description = desc,
                        category = cat,
                        colorHex = color,
                        targetCount = target,
                        unit = unit,
                        reminderTime = reminder
                    )
                    viewModel.editHabit(updated)
                }
                showAddBottomSheet = false
            }
        )
    }
}
