package com.example.smartalarm.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.smartalarm.data.model.HeartRateSample
import com.example.smartalarm.data.model.SleepSession

@Database(
    entities = [SleepSession::class, HeartRateSample::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun sleepSessionDao(): SleepSessionDao
    abstract fun heartRateDao(): HeartRateDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    ALTER TABLE sleep_sessions ADD COLUMN lightMinutes INTEGER NOT NULL DEFAULT 0
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    ALTER TABLE sleep_sessions ADD COLUMN deepMinutes INTEGER NOT NULL DEFAULT 0
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    ALTER TABLE sleep_sessions ADD COLUMN remMinutes INTEGER NOT NULL DEFAULT 0
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    ALTER TABLE sleep_sessions ADD COLUMN wakeMinutes INTEGER NOT NULL DEFAULT 0
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    ALTER TABLE sleep_sessions ADD COLUMN alarmTriggeredAt INTEGER
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    ALTER TABLE sleep_sessions ADD COLUMN triggerReason TEXT
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS heart_rate_samples_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        sessionId INTEGER NOT NULL,
                        bpm INTEGER NOT NULL,
                        timestamp INTEGER NOT NULL,
                        phase TEXT NOT NULL,
                        FOREIGN KEY(sessionId) REFERENCES sleep_sessions(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS index_heart_rate_samples_new_sessionId
                    ON heart_rate_samples_new(sessionId)
                    """.trimIndent()
                )

                db.execSQL("DROP TABLE IF EXISTS heart_rate_samples")
                db.execSQL("ALTER TABLE heart_rate_samples_new RENAME TO heart_rate_samples")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smart_alarm_db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}