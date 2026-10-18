package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Badge
import kotlinx.coroutines.flow.Flow

@Dao
interface BadgeDao {
    @Query("SELECT * FROM badges ORDER BY isUnlocked DESC, id ASC")
    fun getAllBadges(): Flow<List<Badge>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBadges(badges: List<Badge>)

    @Update
    suspend fun updateBadge(badge: Badge)

    @Query("SELECT COUNT(*) FROM badges WHERE isUnlocked = 1")
    suspend fun getUnlockedCount(): Int
}
