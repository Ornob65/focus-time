package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.BlockedNotificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedNotificationDao {
    @Query("SELECT * FROM blocked_notifications ORDER BY timestampMillis DESC")
    fun getAllBlocked(): Flow<List<BlockedNotificationEntity>>

    @Query("SELECT * FROM blocked_notifications WHERE sessionTitle = :sessionTitle ORDER BY timestampMillis DESC")
    fun getBlockedForSession(sessionTitle: String): Flow<List<BlockedNotificationEntity>>

    @Query("SELECT COUNT(*) FROM blocked_notifications")
    fun getBlockedCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: BlockedNotificationEntity): Long

    @Query("DELETE FROM blocked_notifications")
    suspend fun clearAll()
}
