package com.echoran.flowfocus.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.echoran.flowfocus.data.dao.FocusSessionDao
import com.echoran.flowfocus.data.dao.TaskDao
import com.echoran.flowfocus.data.dao.WhitelistedAppDao
import com.echoran.flowfocus.data.model.FocusSessionEntity
import com.echoran.flowfocus.data.model.TaskEntity
import com.echoran.flowfocus.data.model.WhitelistedAppEntity

@Database(entities = [TaskEntity::class, FocusSessionEntity::class, WhitelistedAppEntity::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun whitelistedAppDao(): WhitelistedAppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "flow_focus_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
