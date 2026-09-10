package com.example.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.data.local.dao.AppLimitDao
import com.example.data.local.dao.BlockedNotificationDao
import com.example.data.local.dao.FocusHistoryDao
import com.example.data.local.dao.StudyScheduleDao
import com.example.data.local.entity.AppLimitEntity
import com.example.data.local.entity.BlockedNotificationEntity
import com.example.data.local.entity.FocusHistoryEntity
import com.example.data.local.entity.StudyScheduleEntity

@Database(
    entities = [
        AppLimitEntity::class,
        StudyScheduleEntity::class,
        BlockedNotificationEntity::class,
        FocusHistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appLimitDao(): AppLimitDao
    abstract fun studyScheduleDao(): StudyScheduleDao
    abstract fun blockedNotificationDao(): BlockedNotificationDao
    abstract fun focusHistoryDao(): FocusHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "focus_screentime.db"
                )
                .fallbackToDestructiveMigration()
                .build()
                .also { INSTANCE = it }
            }
        }
    }
}
