package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.FocusHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusHistoryDao {
    @Query("SELECT * FROM focus_history ORDER BY startedAtMillis DESC")
    fun getAllHistory(): Flow<List<FocusHistoryEntity>>

    @Query("SELECT SUM(durationMinutes) FROM focus_history WHERE completedSuccessfully = 1")
    fun getTotalFocusMinutes(): Flow<Int?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: FocusHistoryEntity): Long
}
