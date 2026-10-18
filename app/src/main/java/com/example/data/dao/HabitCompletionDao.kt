package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.HabitCompletion
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitCompletionDao {
    @Query("SELECT * FROM habit_completions WHERE dateString = :dateString")
    fun getCompletionsForDate(dateString: String): Flow<List<HabitCompletion>>

    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId ORDER BY timestamp DESC")
    fun getCompletionsForHabit(habitId: Long): Flow<List<HabitCompletion>>

    @Query("SELECT * FROM habit_completions ORDER BY timestamp DESC")
    fun getAllCompletions(): Flow<List<HabitCompletion>>

    @Query("SELECT * FROM habit_completions WHERE dateString BETWEEN :startDate AND :endDate ORDER BY dateString ASC")
    fun getCompletionsBetween(startDate: String, endDate: String): Flow<List<HabitCompletion>>

    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId AND dateString = :dateString LIMIT 1")
    suspend fun getCompletionForHabitAndDate(habitId: Long, dateString: String): HabitCompletion?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompletion(completion: HabitCompletion): Long

    @Query("DELETE FROM habit_completions WHERE habitId = :habitId AND dateString = :dateString")
    suspend fun deleteCompletionForDate(habitId: Long, dateString: String)

    @Query("SELECT COUNT(*) FROM habit_completions")
    suspend fun getTotalCompletionsCount(): Int

    @Query("SELECT SUM(timeSpentMinutes) FROM habit_completions")
    suspend fun getTotalTimeSpentMinutes(): Int?

    @Query("SELECT DISTINCT dateString FROM habit_completions ORDER BY dateString DESC")
    fun getAllCompletionDates(): Flow<List<String>>
}
