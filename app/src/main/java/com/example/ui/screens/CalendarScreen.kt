package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.HabitCard
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.HabitGreenCompleted
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun CalendarScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val habits by viewModel.habits.collectAsState()
    val allCompletions by viewModel.allCompletions.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()

    var currentCalendar by remember { mutableStateOf(Calendar.getInstance()) }

    val monthYearFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val dayFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    // Map dateString to completion count
    val completionCountsByDate = remember(allCompletions) {
        allCompletions.groupBy { it.dateString }
            .mapValues { it.value.size }
    }

    val daysInMonth = remember(currentCalendar.timeInMillis) {
        val cal = currentCalendar.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val month = cal.get(Calendar.MONTH)
        val days = mutableListOf<CalendarDateItem>()

        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val offset = (firstDayOfWeek - Calendar.MONDAY + 7) % 7

        for (i in 0 until offset) {
            days.add(CalendarDateItem("", 0, isCurrentMonth = false))
        }

        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        for (day in 1..maxDays) {
            cal.set(Calendar.DAY_OF_MONTH, day)
            val dateStr = dayFormat.format(cal.time)
            days.add(CalendarDateItem(dateStr, day, isCurrentMonth = true))
        }
        days
    }

    val completionsForSelectedDay = remember(allCompletions, selectedDate) {
        allCompletions.filter { it.dateString == selectedDate }
    }
    val completedHabitIdsForDay = remember(completionsForSelectedDay) {
        completionsForSelectedDay.map { it.habitId }.toSet()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("calendar_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Calendar Header
        item {
            Column {
                Text(
                    text = "Completion History 📅",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Track your consistency matrix and past daily logs",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Month Navigator
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = {
                                val newCal = currentCalendar.clone() as Calendar
                                newCal.add(Calendar.MONTH, -1)
                                currentCalendar = newCal
                            }) {
                                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month")
                            }

                            Text(
                                text = monthYearFormat.format(currentCalendar.time),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            IconButton(onClick = {
                                val newCal = currentCalendar.clone() as Calendar
                                newCal.add(Calendar.MONTH, 1)
                                currentCalendar = newCal
                            }) {
                                Icon(Icons.Default.ChevronRight, contentDescription = "Next Month")
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Days of week header
                        Row(modifier = Modifier.fillMaxWidth()) {
                            val weekDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                            weekDays.forEach { day ->
                                Text(
                                    text = day,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Days Grid
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(7),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp),
                            userScrollEnabled = false
                        ) {
                            items(daysInMonth) { dateItem ->
                                if (!dateItem.isCurrentMonth) {
                                    Spacer(modifier = Modifier.aspectRatio(1f))
                                } else {
                                    val isSelected = dateItem.dateString == selectedDate
                                    val completionCount = completionCountsByDate[dateItem.dateString] ?: 0

                                    Box(
                                        modifier = Modifier
                                            .aspectRatio(1f)
                                            .padding(2.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    isSelected -> EmeraldPrimary
                                                    completionCount > 0 -> HabitGreenCompleted.copy(alpha = 0.25f)
                                                    else -> Color.Transparent
                                                }
                                            )
                                            .border(
                                                width = if (isSelected) 2.dp else 0.dp,
                                                color = if (isSelected) Color.White else Color.Transparent,
                                                shape = CircleShape
                                            )
                                            .clickable {
                                                if (dateItem.dateString.isNotBlank()) {
                                                    viewModel.setSelectedDate(dateItem.dateString)
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "${dateItem.dayNumber}",
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected || completionCount > 0) FontWeight.Bold else FontWeight.Normal,
                                                color = when {
                                                    isSelected -> Color.White
                                                    completionCount > 0 -> HabitGreenCompleted
                                                    else -> MaterialTheme.colorScheme.onSurface
                                                }
                                            )

                                            if (completionCount > 0 && !isSelected) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(4.dp)
                                                        .clip(CircleShape)
                                                        .background(HabitGreenCompleted)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Selected Date Log Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Habits for $selectedDate",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Surface(
                    color = EmeraldPrimary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${completedHabitIdsForDay.size} Completed",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Habits List for Selected Date
        if (habits.isEmpty()) {
            item {
                Text(
                    text = "No habits created yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(habits) { habit ->
                val isCompleted = completedHabitIdsForDay.contains(habit.id)
                HabitCard(
                    habit = habit,
                    isCompleted = isCompleted,
                    onToggleCompletion = {
                        viewModel.toggleHabitCompletion(habit.id, selectedDate)
                    },
                    onEdit = {
                        viewModel.setSelectedTab(0)
                    },
                    onDelete = {
                        viewModel.deleteHabit(habit.id)
                    }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

data class CalendarDateItem(
    val dateString: String,
    val dayNumber: Int,
    val isCurrentMonth: Boolean
)
