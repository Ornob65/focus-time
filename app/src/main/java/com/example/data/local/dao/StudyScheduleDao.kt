package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.StudyScheduleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyScheduleDao {
    @Query("SELECT * FROM study_schedules ORDER BY startHour ASC, startMinute ASC")
    fun getAllSchedules(): Flow<List<StudyScheduleEntity>>

    @Query("SELECT * FROM study_schedules WHERE isEnabled = 1")
    fun getActiveSchedules(): Flow<List<StudyScheduleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(schedule: StudyScheduleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(schedules: List<StudyScheduleEntity>)

    @Update
    suspend fun update(schedule: StudyScheduleEntity)

    @Query("DELETE FROM study_schedules WHERE id = :id")
    suspend fun deleteById(id: Long)
}
