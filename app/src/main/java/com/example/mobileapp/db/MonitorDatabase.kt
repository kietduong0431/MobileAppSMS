package com.example.mobileapp.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.mobileapp.model.MonitorLog

@Database(entities = [MonitorLog::class], version = 1, exportSchema = false)
abstract class MonitorDatabase : RoomDatabase() {

    abstract fun monitorLogDao(): MonitorLogDao

    companion object {
        @Volatile
        private var INSTANCE: MonitorDatabase? = null

        fun getDatabase(context: Context): MonitorDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MonitorDatabase::class.java,
                    "monitor_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
