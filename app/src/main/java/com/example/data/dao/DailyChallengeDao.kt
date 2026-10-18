package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.DailyChallenge
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyChallengeDao {
    @Query("SELECT * FROM daily_challenges WHERE dateString = :dateString")
    fun getChallengeForDate(dateString: String): Flow<DailyChallenge?>

    @Query("SELECT * FROM daily_challenges WHERE dateString = :dateString")
    suspend fun getChallengeForDateDirect(dateString: String): DailyChallenge?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateChallenge(challenge: DailyChallenge)
}
