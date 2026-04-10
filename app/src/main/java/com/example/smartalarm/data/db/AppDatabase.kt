package com.example.smartalarm.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.smartalarm.data.model.AlarmConfig
import com.example.smartalarm.data.model.HeartRateSample
import com.example.smartalarm.data.model.SleepSession

@Database(
    entities = [SleepSession::class, HeartRateSample::class, AlarmConfig::class],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun sleepSessionDao(): SleepSessionDao
    abstract fun heartRateDao(): HeartRateDao
    abstract fun alarmConfigDao(): AlarmConfigDao

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

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS alarm_configs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        hour24 INTEGER NOT NULL,
                        minute INTEGER NOT NULL,
                        smartWindowMinutes INTEGER NOT NULL,
                        repeatDays TEXT NOT NULL,
                        soundName TEXT NOT NULL,
                        volumePercent INTEGER NOT NULL,
                        vibrationMode TEXT NOT NULL,
                        snoozeEnabled INTEGER NOT NULL,
                        snoozeMinutes INTEGER NOT NULL,
                        snoozeMaxRepeats INTEGER NOT NULL,
                        isEnabled INTEGER NOT NULL,
                        isSelected INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    ALTER TABLE sleep_sessions ADD COLUMN replayMode TEXT NOT NULL DEFAULT 'LEGACY_UNKNOWN'
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    UPDATE sleep_sessions
                    SET replayMode = 'LEGACY_UNKNOWN'
                    WHERE replayMode = 'CSV_REALTIME'
                    """.trimIndent()
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smart_alarm_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
