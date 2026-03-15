package com.example.smartalarm.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.smartalarm.data.model.HeartRateSample
import com.example.smartalarm.data.model.SleepSession

@Database(entities = [HeartRateSample::class, SleepSession::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun heartRateDao(): HeartRateDao
    abstract fun sleepSessionDao(): SleepSessionDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smart_alarm_db"
                ).build().also { instance = it }
            }
    }
}